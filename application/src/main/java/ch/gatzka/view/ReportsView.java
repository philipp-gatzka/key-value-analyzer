package ch.gatzka.view;

import ch.gatzka.enums.GameMode;
import ch.gatzka.repository.view.ItemPriceViewRepository;
import ch.gatzka.repository.view.KeyReportViewRepository;
import ch.gatzka.repository.view.LootReportViewRepository;
import ch.gatzka.security.AuthenticatedAccount;
import ch.gatzka.tables.records.ItemPriceViewRecord;
import ch.gatzka.tables.records.KeyReportViewRecord;
import ch.gatzka.tables.records.LootReportViewRecord;
import ch.qos.logback.core.joran.sanity.Pair;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.jooq.Result;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.time.format.DateTimeFormatter;

@PageTitle("Reports")
@Route("reports")
@Menu(order = 4, icon = LineAwesomeIconUrl.LIST_ALT_SOLID)
@AnonymousAllowed
public class ReportsView extends VerticalLayout {

    private final Grid<KeyReportViewRecord> grid = new Grid<>(KeyReportViewRecord.class, false);

    private final KeyReportViewRepository keyReportViewRepository;

    private final LootReportViewRepository lootReportViewRepository;

    private final ItemPriceViewRepository itemPriceViewRepository;

    public ReportsView(KeyReportViewRepository keyReportViewRepository, LootReportViewRepository lootReportViewRepository, ItemPriceViewRepository itemPriceViewRepository, AuthenticatedAccount authenticatedAccount) {
        this.keyReportViewRepository = keyReportViewRepository;
        this.lootReportViewRepository = lootReportViewRepository;
        this.itemPriceViewRepository = itemPriceViewRepository;

        GameMode gameMode;
        if (authenticatedAccount.get().isPresent()) {
            gameMode = authenticatedAccount.get().get().account().getGameMode();
        } else {
            gameMode = GameMode.PvP;
        }

        setSizeFull();
        createGrid();

        grid.setItems(keyReportViewRepository.readByGameMode(gameMode));
    }

    private void createGrid() {
        grid.setSizeFull();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        grid.addComponentColumn(entry -> {
            Image icon = new Image(entry.getImageLink(), entry.getKeyName());
            icon.setMaxHeight(50, Unit.PIXELS);
            VerticalLayout layout = new VerticalLayout(icon);
            layout.setPadding(false);
            return layout;
        }).setHeader("Icon");

        grid.addColumn("keyName").setHeader("Name");
        grid.addColumn("reportedAt").setRenderer(new TextRenderer<>(entry -> entry.getReportedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))).setHeader("Reported At");
        grid.addColumn("reportedBy").setRenderer(new TextRenderer<>(entry -> "User#" + entry.getReportedBy())).setHeader("Reported By");
        grid.addColumn("itemCount").setHeader("Items Found");


        grid.setItemDetailsRenderer(new ComponentRenderer<>(this::createItemDetailRenderer));

        add(grid);
    }

    private VerticalLayout createItemDetailRenderer(KeyReportViewRecord keyReport) {
        Result<LootReportViewRecord> lootReports = lootReportViewRepository.readByKeyReportId(keyReport.getKeyReportId());

        Grid<LootReportViewRecord> grid = new Grid<>(LootReportViewRecord.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);


        grid.addComponentColumn(entry -> {
            Image icon = new Image(entry.getImageLink(), entry.getName());
            icon.setMaxHeight(50, Unit.PIXELS);
            VerticalLayout layout = new VerticalLayout(icon);
            layout.setPadding(false);
            return layout;
        }).setHeader("Icon");

        grid.addColumn("name").setHeader("Name");
        grid.addColumn("count").setHeader("Count");

        grid.addColumn(entry -> getItemValue(keyReport.getGameMode(), entry, entry.getCount()).getFirst()).setHeader("Value");

        grid.setItems(lootReports);

        return new VerticalLayout(grid);
    }

    private kotlin.Pair<String, Integer> getItemValue(GameMode gameMode, LootReportViewRecord report, int count) {

        ItemPriceViewRecord itemPrices = itemPriceViewRepository.findByItemId(report.getItemId()).orElseThrow(() -> new IllegalArgumentException("Item prices not found"));

        int itemValue;
        String itemValueString;
        if (gameMode == GameMode.PvP) {
            if (itemPrices.getPvpFleaValue() > itemPrices.getPvpTraderValue()) {
                itemValueString = "₽ " + itemPrices.getPvpFleaValue() * count;
                itemValue = itemPrices.getPvpFleaValue() * count;
            } else {
                itemValueString = itemPrices.getPvpTraderValueCurrency() + " " + itemPrices.getPvpTraderValue() * count;
                itemValue = itemPrices.getPvpTraderValue() * count;
            }
        } else {
            if (itemPrices.getPveFleaValue() > itemPrices.getPveTraderValue()) {
                itemValueString = "₽ " + itemPrices.getPveFleaValue() * count;
                itemValue = itemPrices.getPveFleaValue() * count;
            } else {
                itemValueString = itemPrices.getPveTraderValueCurrency() + " " + itemPrices.getPveTraderValue() * count;
                itemValue = itemPrices.getPveTraderValue() * count;
            }
        }

        return new kotlin.Pair<>(itemValueString, itemValue);
    }

}
