package ch.gatzka;

import ch.gatzka.repository.*;
import ch.gatzka.service.GraphQlService;
import ch.gatzka.tables.records.*;
import ch.gatzka.type.ItemType;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final GraphQlService service;

    private final ItemRepository itemRepository;

    private final ItemTypeRepository itemTypeRepository;

    private final TypeRepository typeRepository;

    private final DefaultDSLContext dslContext;
    private final KeyRepository keyRepository;
    private final VendorRepository vendorRepository;
    private final ItemSaleRepository itemSaleRepository;

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("Initializing data");
        try {
            measuredProcessOf("fetching item ids", this::fetchItemIds);
            measuredProcessOf("updating item data", this::fetchItemData);
            measuredProcessOf("updating item types", this::fetchItemTypes);
            measuredProcessOf("updating keys", this::fetchKeys);
            measuredProcessOf("updating item prices", this::forceFetchItemPrices);
        } catch (Exception e) {
            log.error("Error initializing data", e);
        }
        log.info("Data initialization took {} ms", System.currentTimeMillis() - startTime);
    }

    private void measuredProcessOf(String name, Runnable runnable) {
        long startTime = System.currentTimeMillis();
        runnable.run();
        log.info("Finished {} in {} ms", name, System.currentTimeMillis() - startTime);
    }

    private void fetchItemIds() {
        List<FetchItemIdsQuery.Item> data = service.runQuery(new FetchItemIdsQuery()).items.stream().filter(Objects::nonNull).toList();
        List<String> existingItems = itemRepository.readAll().map(ItemRecord::getTarkovId);
        List<String> newIds = data.stream().map(item -> item.id).filter(id -> !existingItems.contains(id)).toList();

        for (String id : newIds) {
            itemRepository.insert(item -> item.setTarkovId(id));
        }
    }

    private void fetchItemData() {
        List<FetchItemDataQuery.Item> data = service.runQuery(new FetchItemDataQuery()).items.stream().filter(Objects::nonNull).toList();
        Map<String, ItemRecord> items = itemRepository.readAll().intoMap(ItemRecord::getTarkovId);

        List<ItemRecord> updatedItems = data.stream().map(itemData -> new Pair<>(itemData, items.get(itemData.id))).filter(pair -> {
            FetchItemDataQuery.Item itemData = pair.getFirst();
            Instant instant = Instant.parse(itemData.updated);
            LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            ItemRecord item = pair.getSecond();
            return item.getUpdated() == null || updated.isAfter(item.getUpdated());
        }).map(pair -> {
            FetchItemDataQuery.Item itemData = pair.getFirst();
            ItemRecord item = pair.getSecond();
            Instant instant = Instant.parse(itemData.updated);
            LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            item.setName(itemData.name);
            item.setImageLink(itemData.image8xLink);
            item.setUpdated(updated);
            return item;
        }).toList();

        dslContext.batchUpdate(updatedItems).execute();
    }

    private void fetchItemTypes() {
        List<FetchItemTypesQuery.Item> data = service.runQuery(new FetchItemTypesQuery()).items.stream().filter(Objects::nonNull).toList();
        Map<String, ItemRecord> items = itemRepository.readAll().intoMap(ItemRecord::getTarkovId);

        List<Pair<FetchItemTypesQuery.Item, ItemRecord>> itemsToUpdate = data.stream().map(itemData -> new Pair<>(itemData, items.get(itemData.id))).filter(pair -> {
            FetchItemTypesQuery.Item itemData = pair.getFirst();
            Instant instant = Instant.parse(itemData.updated);
            LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            ItemRecord item = pair.getSecond();
            return updated.isAfter(item.getUpdated());
        }).toList();

        for (Pair<FetchItemTypesQuery.Item, ItemRecord> pair : itemsToUpdate) {
            FetchItemTypesQuery.Item itemData = pair.getFirst();
            ItemRecord item = pair.getSecond();

            itemTypeRepository.deleteByItemId(item.getId());
            for (ItemType typeData : itemData.types) {
                Optional<TypeRecord> optionalType = typeRepository.findByName(typeData.rawValue);

                TypeRecord type;
                if (optionalType.isEmpty()) {
                    Integer typeId = typeRepository.insertWithSequence(typeRecord -> typeRecord.setName(typeData.rawValue));
                    type = typeRepository.findById(typeId).orElseThrow();
                } else {
                    type = optionalType.get();
                }

                itemTypeRepository.insert(itemType -> itemType.setItemId(item.getId()).setTypeId(type.getId()));
            }
        }
    }

    private void fetchKeys() {
        List<FetchKeysQuery.Item> data = service.runQuery(new FetchKeysQuery()).items.stream().filter(Objects::nonNull).toList();

        Map<String, ItemRecord> items = itemRepository.readAll().intoMap(ItemRecord::getTarkovId);
        Map<String, KeyRecord> keys = keyRepository.readAll().intoMap(key -> itemRepository.findById(key.getItemId()).orElseThrow().getTarkovId());

        data.stream().filter(itemData -> {
            Instant instant = Instant.parse(itemData.updated);
            LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            ItemRecord item = items.get(itemData.id);
            return updated.isAfter(item.getUpdated());
        }).forEach(itemData -> {
            if (keys.containsKey(itemData.id)) {
                KeyRecord key = keys.get(itemData.id);
                key.setUses(itemData.properties.onItemPropertiesKey.uses);
                key.update();
            } else {
                keyRepository.insertWithSequence(rec -> rec.setItemId(items.get(itemData.id).getId()).setUses(itemData.properties.onItemPropertiesKey.uses));
            }
        });
    }

    private void forceFetchItemPrices() {
        fetchItemPrices(true);
    }

    private void fetchItemPrices(boolean forceUpdate) {
        List<FetchItemPricesQuery.Item> data = service.runQuery(new FetchItemPricesQuery()).items.stream().filter(Objects::nonNull).toList();

        Map<String, ItemRecord> items = itemRepository.readAll().intoMap(ItemRecord::getTarkovId);

        List<FetchItemPricesQuery.Item> dataToUpdate = data.stream().filter(itemData -> {
            Instant instant = Instant.parse(itemData.updated);
            LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            ItemRecord item = items.get(itemData.id);
            return updated.isAfter(item.getUpdated()) || forceUpdate;
        }).toList();

        int totalItems = dataToUpdate.size();
        int processedItems = 0;
        int lastLoggedPercentage = -1;

        for (FetchItemPricesQuery.Item itemData : dataToUpdate) {
            ItemRecord item = items.get(itemData.id);
            if (!Objects.equals(item.getBuyPrice(), itemData.lastLowPrice)) {
                item.setBuyPrice(itemData.lastLowPrice);
                item.update();
            }

            for (FetchItemPricesQuery.SellFor sale : itemData.sellFor) {
                Optional<VendorRecord> optionalVendor = vendorRepository.findByName(sale.vendor.name);
                if (optionalVendor.isEmpty()) {
                    Integer vendorId = vendorRepository.insertWithSequence(vendorRecord -> vendorRecord.setName(sale.vendor.name));
                    optionalVendor = vendorRepository.findById(vendorId);
                }
                VendorRecord vendor = optionalVendor.orElseThrow();

                Optional<ItemSaleRecord> optionalItemSale = itemSaleRepository.findByItemIdAndVendorId(item.getId(), vendor.getId());
                if (optionalItemSale.isEmpty()) {
                    itemSaleRepository.insert(entity -> entity.setItemId(item.getId()).setVendorId(vendor.getId()).setSellPrice(sale.priceRUB));
                } else {
                    ItemSaleRecord itemSale = optionalItemSale.get();
                    if (!Objects.equals(itemSale.getSellPrice(), sale.priceRUB)) {
                        itemSale.setSellPrice(sale.priceRUB);
                        itemSale.update();
                    }
                }
            }

            processedItems++;
            int currentPercentage = (processedItems * 100) / totalItems;
            if (currentPercentage != lastLoggedPercentage) {
                lastLoggedPercentage = currentPercentage;
                log.info("Item price update progress: {}%", currentPercentage);
            }
        }
    }

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES, initialDelay = 10)
    private void updateItemPrices() {
        long startTime = System.currentTimeMillis();
        log.info("Updating item prices");
        fetchItemPrices(false);
        log.info("Finished updating item prices after {} ms", System.currentTimeMillis() - startTime);

        LocalDateTime nextUpdate = LocalDateTime.now().plusMinutes(10);
        log.info("Next update scheduled at {}", nextUpdate);
    }
}
