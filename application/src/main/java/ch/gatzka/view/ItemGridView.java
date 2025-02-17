package ch.gatzka.view;

import ch.gatzka.repository.TypeRepository;
import ch.gatzka.repository.view.ItemGridViewRepository;
import ch.gatzka.tables.records.ItemGridViewRecord;
import ch.gatzka.tables.records.TypeRecord;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.jooq.Condition;
import org.jooq.Result;
import org.vaadin.lineawesome.LineAwesomeIconUrl;

import java.util.HashMap;
import java.util.Map;

import static ch.gatzka.Tables.ITEM_GRID_VIEW;

@PageTitle("Items")
@Route("items")
@Menu(order = 1, icon = LineAwesomeIconUrl.BOX_SOLID)
@AnonymousAllowed
public class ItemGridView extends VerticalLayout {

    private final Grid<ItemGridViewRecord> grid = new Grid<>(ItemGridViewRecord.class, false);

    private final ItemGridViewRepository itemGridViewRepository;

    private final Map<String, Condition> conditions = new HashMap<>();

    private final TypeRepository typeRepository;

    public ItemGridView(ItemGridViewRepository itemGridViewRepository, TypeRepository typeRepository) {
        this.itemGridViewRepository = itemGridViewRepository;
        this.typeRepository = typeRepository;

        createHeader();
        createGrid();

        setSizeFull();

        refreshGrid();
    }

    private void createHeader() {
        TextField nameField = new TextField("Name");
        nameField.setWidthFull();
        nameField.setPlaceholder("Filter by name");
        nameField.addValueChangeListener(event -> {
            String newValue = event.getValue();
            if (newValue == null || newValue.isEmpty()) {
                conditions.remove("name");
            } else {
                conditions.put("name", ITEM_GRID_VIEW.NAME.likeIgnoreCase("%" + newValue + "%"));
            }
        });

        ComboBox<String> typeField = new ComboBox<>("Type");
        typeField.setWidthFull();
        typeField.setPlaceholder("Filter by type");
        typeField.setItems(typeRepository.readAll().map(TypeRecord::getName));
        typeField.addValueChangeListener(event -> {
            String newValue = event.getValue();
            if (newValue == null || newValue.isEmpty()) {
                conditions.remove("type");
            } else {
                conditions.put("type", ITEM_GRID_VIEW.TYPES.likeIgnoreCase("%" + newValue + "%"));
            }
        });

        Button resetButton = new Button("Reset");
        resetButton.setWidthFull();
        resetButton.setIcon(new Icon(VaadinIcon.TRASH));
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(_ -> {
            nameField.clear();
            typeField.clear();
            conditions.clear();
            refreshGrid();
        });

        Button searchButton = new Button("Search");
        searchButton.setWidthFull();
        searchButton.setIcon(new Icon(VaadinIcon.SEARCH));
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchButton.addClickListener(_ -> refreshGrid());
        searchButton.addClickShortcut(Key.ENTER);

        VerticalLayout buttonLayout = new VerticalLayout(resetButton, searchButton);
        buttonLayout.setSpacing(false);
        buttonLayout.setPadding(false);
        buttonLayout.setWidth("min-content");

        HorizontalLayout header = new HorizontalLayout(nameField, typeField, buttonLayout);
        header.setWidthFull();
        header.setAlignSelf(Alignment.END, nameField);
        header.setAlignSelf(Alignment.END, typeField);

        add(header);
    }

    private void createGrid() {
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addComponentColumn(data -> {
            Image image = new Image(data.getImageLink() == null ? data.getName() : data.getImageLink(), data.getName());
            image.setMaxHeight(50, Unit.PIXELS);
            return image;
        }).setHeader("Image");
        grid.addColumn("name").setHeader("Name");
        grid.addColumn(data -> data.getTypes().replace("|", ", ")).setHeader("Types");
        grid.addColumn("buyPrice").setRenderer(new TextRenderer<>(ItemGridViewRecord::getBuyPriceRouble)).setHeader("Price");
        grid.addColumn("sellPrice").setRenderer(new TextRenderer<>(ItemGridViewRecord::getSellPriceRouble)).setHeader("Sell Price");
        grid.addColumn("sellAt").setHeader("Sell At");

        add(grid);
    }

    private void refreshGrid() {
        Condition[] conditions = this.conditions.values().toArray(Condition[]::new);
        Result<ItemGridViewRecord> records = itemGridViewRepository.read(conditions);
        grid.setItems(records);
    }

}
