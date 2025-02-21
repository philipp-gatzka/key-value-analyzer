package ch.gatzka;

import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.ItemRepository;
import ch.gatzka.repository.ItemTagRepository;
import ch.gatzka.repository.KeyRepository;
import ch.gatzka.repository.TagRepository;
import ch.gatzka.service.GraphQlService;
import ch.gatzka.tables.records.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import kotlin.Pair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ch.gatzka.Tables.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final String PVE_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/pve/items/all?x-api-key={API_KEY}";

    private static final String PVP_ALL_ITEMS = "https://api.tarkov-market.app/api/v1/items/all?x-api-key={API_KEY}";

    @Value("${api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ItemRepository itemRepository;

    private final TagRepository tagRepository;

    private final ItemTagRepository itemTagRepository;

    private final GraphQlService service;
    private final KeyRepository keyRepository;
    private final DefaultDSLContext dslContext;

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        try {
            updateData();
            fetchKeys();
        } catch (Exception e) {
            log.error("Error updating data", e);
        }
    }

    private void generateRandomData() {
        final int KEY_REPORT_COUNT = 1000;

        List<KeyReportRecord> newKeyReports = new ArrayList<>();

        Result<KeyRecord> keys = dslContext.selectFrom(KEY).fetch();

        Random random = new Random();

        for (int i = 0; i < KEY_REPORT_COUNT; i++) {

            int randomIndex = new Random().nextInt(keys.size());
            Integer randomKeyId = keys.get(randomIndex).getId();
            
            newKeyReports.add(dslContext.newRecord(KEY_REPORT)
                    .setKeyId(randomKeyId)
                    .setMode(GameMode.PvE)
                    .setReportedAt(LocalDateTime.now())
                    .setAccountId(1));

        }

        dslContext.batchInsert(newKeyReports).execute();
        
        final int LOOT_REPORT_COUNT = KEY_REPORT_COUNT * 10;

        Result<KeyReportRecord> keyReports = dslContext.selectFrom(KEY_REPORT).fetch();
        Result<ItemRecord> items = dslContext.selectFrom(ITEM).fetch();

        List<LootReportRecord> lootReports = new ArrayList<>();
        for (int i = 0; i < LOOT_REPORT_COUNT; i++) {

            int itemIndex = new Random().nextInt(items.size());
            ItemRecord item = items.get(itemIndex);

            int keyReportIndex = new Random().nextInt(keyReports.size());
            KeyReportRecord keyReport = keyReports.get(keyReportIndex);

            lootReports.add(dslContext.newRecord(LOOT_REPORT)
                    .setKeyReportId(keyReport.getId())
                    .setItemId(item.getId())
                    .setCount(random.nextInt(10))
            );

        }

        dslContext.batchInsert(lootReports).execute();

    }

    private void updateData() throws Exception {
        String pveJsonResponse = restTemplate.getForObject(PVE_ALL_ITEMS, String.class, apiKey);
        String pvpJsonResponse = restTemplate.getForObject(PVP_ALL_ITEMS, String.class, apiKey);

        Response[] pveObjects = objectMapper.readValue(pveJsonResponse, Response[].class);
        Response[] pvpObjects = objectMapper.readValue(pvpJsonResponse, Response[].class);

        Map<String, Pair<Response, Response>> data = new HashMap<>();

        for (Response pveObject : pveObjects) {
            if (data.containsKey(pveObject.uid)) {
                Pair<Response, Response> pair = data.get(pveObject.uid);
                pair = new Pair<>(pveObject, pair.getSecond());
                data.put(pveObject.uid, pair);
            } else {
                data.put(pveObject.uid, new Pair<>(pveObject, null));
            }
        }

        for (Response pvpObject : pvpObjects) {
            if (data.containsKey(pvpObject.uid)) {
                Pair<Response, Response> pair = data.get(pvpObject.uid);
                pair = new Pair<>(pair.getFirst(), pvpObject);
                data.put(pvpObject.uid, pair);
            } else {
                data.put(pvpObject.uid, new Pair<>(null, pvpObject));
            }
        }


        int totalItems = data.size();
        int processedItems = 0;
        int lastLoggedPercentage = -1;

        int insertedCount = 0;
        int updatedCount = 0;
        for (Pair<Response, Response> object : data.values()) {
            Pair<Integer, Integer> counts = updateItem(object.getFirst(), object.getSecond());
            insertedCount += counts.getFirst();
            updatedCount += counts.getSecond();
            processedItems++;
            int currentPercentage = (processedItems * 100) / totalItems;
            if (currentPercentage != lastLoggedPercentage) {
                lastLoggedPercentage = currentPercentage;
                log.info("Data update progress: {}%", currentPercentage);
            }
        }
        log.info("Data update finished. {} items inserted, {} items updated", insertedCount, updatedCount);
    }

    private Pair<Integer, Integer> updateItem(Response pveData, Response pvpData) {
        int insertedCount = 0;
        int updatedCount = 0;

        Instant instant = pveData.updated.isEmpty() ? Instant.now() : Instant.parse(pveData.updated);
        LocalDateTime updated = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        Optional<ItemRecord> optionalItem = itemRepository.findByTarkovId(pveData.bsgId);

        if (optionalItem.isEmpty()) {
            Integer itemId = itemRepository.insertWithSequence(item -> item
                    .setTarkovMarketId(pveData.uid)
                    .setName(pveData.name)
                    .setBannedOnFlea(pveData.bannedOnFlea)
                    .setHaveMarketData(pveData.haveMarketData)
                    .setShortName(pveData.shortName)
                    .setPvePrice(pveData.price)
                    .setPvpPrice(pvpData.price)
                    .setPveBasePrice(pveData.basePrice)
                    .setPvpBasePrice(pvpData.basePrice)
                    .setPveAvg24hPrice(pveData.avg24hPrice)
                    .setPvpAvg24hPrice(pvpData.avg24hPrice)
                    .setPveAvg7daysPrice(pveData.avg7daysPrice)
                    .setPvpAvg7daysPrice(pvpData.avg7daysPrice)
                    .setPveTraderName(pveData.traderName)
                    .setPvpTraderName(pvpData.traderName)
                    .setPveTraderPrice(pveData.traderPrice)
                    .setPvpTraderPrice(pvpData.traderPrice)
                    .setPveTraderPriceCurrency(pveData.traderPriceCur)
                    .setPvpTraderPriceCurrency(pvpData.traderPriceCur)
                    .setPveTraderPriceRouble(pveData.traderPriceRub)
                    .setPvpTraderPriceRouble(pvpData.traderPriceRub)
                    .setPveDiff24h(pveData.diff24h)
                    .setPvpDiff24h(pvpData.diff24h)
                    .setPveDiff7days(pveData.diff7days)
                    .setPvpDiff7days(pvpData.diff7days)
                    .setUpdated(updated)
                    .setSlots(pveData.slots)
                    .setIcon(pveData.icon)
                    .setLink(pveData.link)
                    .setWikiLink(pveData.wikiLink)
                    .setImageLink(pveData.img)
                    .setImageBigLink(pveData.imgBig)
                    .setTarkovId(pveData.bsgId)
                    .setIsFunctional(pveData.isFunctional)
                    .setReference(pveData.reference)
            );

            for (String name : pveData.tags) {
                TagRecord tag = getTag(name);
                itemTagRepository.insert(itemTag -> itemTag.setItemId(itemId).setTagId(tag.getId()));
            }
            insertedCount++;
        } else {
            ItemRecord item = optionalItem.get();

            if (!item.getUpdated().equals(updated)) {
                item.setUpdated(updated);
                item.setName(pveData.name);
                item.setBannedOnFlea(pveData.bannedOnFlea);
                item.setHaveMarketData(pveData.haveMarketData);
                item.setShortName(pveData.shortName);
                item.setPvePrice(pveData.price);
                item.setPvpPrice(pvpData.price);
                item.setPveBasePrice(pveData.basePrice);
                item.setPvpBasePrice(pvpData.basePrice);
                item.setPveAvg24hPrice(pveData.avg24hPrice);
                item.setPvpAvg24hPrice(pvpData.avg24hPrice);
                item.setPveAvg7daysPrice(pveData.avg7daysPrice);
                item.setPvpAvg7daysPrice(pvpData.avg7daysPrice);
                item.setPveTraderName(pveData.traderName);
                item.setPvpTraderName(pvpData.traderName);
                item.setPveTraderPrice(pveData.traderPrice);
                item.setPvpTraderPrice(pvpData.traderPrice);
                item.setPveTraderPriceCurrency(pveData.traderPriceCur);
                item.setPvpTraderPriceCurrency(pvpData.traderPriceCur);
                item.setPveTraderPriceRouble(pveData.traderPriceRub);
                item.setPvpTraderPriceRouble(pvpData.traderPriceRub);
                item.setPveDiff24h(pveData.diff24h);
                item.setPvpDiff24h(pvpData.diff24h);
                item.setPveDiff7days(pveData.diff7days);
                item.setPvpDiff7days(pvpData.diff7days);
                item.setUpdated(updated);
                item.setSlots(pveData.slots);
                item.setIcon(pveData.icon);
                item.setLink(pveData.link);
                item.setWikiLink(pveData.wikiLink);
                item.setImageLink(pveData.img);
                item.setImageBigLink(pveData.imgBig);
                item.setTarkovId(pveData.bsgId);
                item.setIsFunctional(pveData.isFunctional);
                item.setReference(pveData.reference);
                item.update();

                itemTagRepository.deleteByItemId(item.getId());

                for (String name : pveData.tags) {
                    TagRecord tag = getTag(name);
                    itemTagRepository.insert(itemTag -> itemTag.setItemId(item.getId()).setTagId(tag.getId()));
                }

                updatedCount++;
            }
        }
        return new Pair<>(insertedCount, updatedCount);
    }

    private TagRecord getTag(String name) {
        Optional<TagRecord> optionalTag = tagRepository.findByName(name);
        if (optionalTag.isEmpty()) {
            tagRepository.insertWithSequence(tagRecord -> tagRecord.setName(name));
            optionalTag = tagRepository.findByName(name);
        }
        return optionalTag.orElseThrow(() -> new RuntimeException("Tag with name " + name + " not found"));
    }

    private record Response(String uid, String name, Boolean bannedOnFlea, Boolean haveMarketData, String shortName, Integer price, Integer basePrice, Integer avg24hPrice, Integer avg7daysPrice, String traderName, Integer traderPrice,
                            String traderPriceCur, Integer traderPriceRub, String updated, Integer slots, String icon, String link, String wikiLink, String img, String imgBig, String bsgId, String[] tags, Double diff24h, Double diff7days,
                            Boolean isFunctional, String reference) {

    }

    private void fetchKeys() {
        List<FetchKeysQuery.Item> data = service.runQuery(new FetchKeysQuery()).items.stream().filter(Objects::nonNull).toList();

        data.forEach(itemData -> {
            Optional<ItemRecord> optionalItem = itemRepository.findByTarkovId(itemData.id);

            if (optionalItem.isPresent()) {
                ItemRecord item = optionalItem.get();

                Optional<KeyRecord> optionalKey = keyRepository.findByItemId(item.getId());

                if (optionalKey.isPresent()) {
                    // Update
                    KeyRecord key = optionalKey.get();
                    key.setUses(itemData.properties.onItemPropertiesKey.uses);
                    key.update();
                } else {
                    // Insert
                    keyRepository.insertWithSequence(entry -> entry.setItemId(item.getId()).setUses(itemData.properties.onItemPropertiesKey.uses));
                }
            }
        });
    }



    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES, initialDelay = 10)
    private void updateItemPrices() {
        long startTime = System.currentTimeMillis();
        log.info("Starting update item prices");
        try {
            updateData();
            fetchKeys();
        } catch (Exception e) {
            log.error("Error updating data", e);
        }
        log.info("Finished updating item prices after {} ms", System.currentTimeMillis() - startTime);

        LocalDateTime nextUpdate = LocalDateTime.now().plusMinutes(10);
        log.info("Next update scheduled at {}", nextUpdate);
    }
}
