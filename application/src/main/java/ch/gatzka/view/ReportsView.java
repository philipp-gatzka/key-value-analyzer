package ch.gatzka.view;

import ch.gatzka.repository.LootReportRepository;
import ch.gatzka.repository.view.KeyReportViewRepository;
import ch.gatzka.repository.view.LootReportViewRepository;
import ch.gatzka.tables.records.KeyReportRecord;
import ch.gatzka.tables.records.KeyReportViewRecord;
import ch.gatzka.tables.records.LootReportRecord;
import ch.gatzka.tables.records.LootReportViewRecord;
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
import org.vaadin.lineawesome.LineAwesomeIconUrl;

@PageTitle("Reports")
@Route("reports")
@Menu(order = 1, icon = LineAwesomeIconUrl.LIST_ALT_SOLID)
@AnonymousAllowed
public class ReportsView extends VerticalLayout {

    private final KeyReportViewRepository keyReportViewRepository;
    private final LootReportRepository lootReportRepository;
    private final LootReportViewRepository lootReportViewRepository;

    public ReportsView(KeyReportViewRepository keyReportViewRepository, LootReportRepository lootReportRepository, LootReportViewRepository lootReportViewRepository) {
        this.keyReportViewRepository = keyReportViewRepository;

        createGrid();
        setSizeFull();
        this.lootReportRepository = lootReportRepository;
        this.lootReportViewRepository = lootReportViewRepository;
    }

    private void createGrid() {
        Grid<KeyReportViewRecord> grid = new Grid<>(KeyReportViewRecord.class, false);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addComponentColumn(data -> {
            Image image = new Image(data.getImageLink() == null ? data.getName() : data.getImageLink(), data.getName());
            image.setMaxHeight(50, Unit.PIXELS);
            return image;
        }).setHeader("Image");
        grid.addColumn("name").setHeader("Name");
        grid.addColumn("itemCount").setHeader("Items Found");
        grid.addColumn("totalValue").setRenderer(new TextRenderer<>(item -> "₽ " + item.getTotalValue())).setHeader("Total Value");

        grid.setItemDetailsRenderer(createKeyReportDetailsRenderer());

        grid.setSizeFull();
        grid.setItems(keyReportViewRepository.readAll());
        add(grid);
    }

    private ComponentRenderer<KeyReportDetails, KeyReportViewRecord> createKeyReportDetailsRenderer() {
        return new ComponentRenderer<>(KeyReportDetails::new, KeyReportDetails::setKeyReport);
    }

    private class KeyReportDetails extends VerticalLayout {

        private final Grid<LootReportViewRecord> grid = new Grid<>(LootReportViewRecord.class, false);

        public KeyReportDetails() {
            setSpacing(false);
            setPadding(false);

            configureGrid();
            setSizeFull();

            add(grid);
        }

        public void setKeyReport(KeyReportViewRecord keyReport) {
            grid.setItems(lootReportViewRepository.findByKeyReportId(keyReport.getId()));
        }

        private void configureGrid() {
            grid.addComponentColumn(data -> {
                Image image = new Image(data.getImageLink() != null ? data.getImageLink() : data.getName(), data.getName());
                image.setMaxHeight(50, Unit.PIXELS);
                return image;
            }).setHeader("Image");
            grid.addColumn("name").setHeader("Name").setAutoWidth(true);
            grid.addColumn("count").setHeader("Count").setAutoWidth(true);
            grid.addColumn("lootValue").setHeader("Loot Value").setAutoWidth(true);
        }
    }

}
