package ch.gatzka.view;

import ch.gatzka.repository.TypeRepository;
import ch.gatzka.repository.view.ItemGridViewRepository;
import ch.gatzka.repository.view.KeyGridViewRepository;
import ch.gatzka.tables.records.ItemGridViewRecord;
import ch.gatzka.tables.records.KeyGridViewRecord;
import ch.gatzka.tables.records.TypeRecord;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.SortOrderProvider;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.SortDirection;
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
import java.util.stream.Stream;

import static ch.gatzka.Tables.ITEM_GRID_VIEW;
import static ch.gatzka.Tables.KEY_GRID_VIEW;

@PageTitle("Keys")
@Route("keys")
@Menu(order = 1, icon = LineAwesomeIconUrl.KEY_SOLID)
@AnonymousAllowed
public class KeyGridView extends VerticalLayout {

    private final Grid<KeyGridViewRecord> grid = new Grid<>(KeyGridViewRecord.class, false);

    private final KeyGridViewRepository keyGridViewRepository;

    private final Map<String, Condition> conditions = new HashMap<>();

    public KeyGridView(KeyGridViewRepository keyGridViewRepository) {
        this.keyGridViewRepository = keyGridViewRepository;

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
                conditions.put("name", KEY_GRID_VIEW.NAME.likeIgnoreCase("%" + newValue + "%"));
            }
        });

        Button resetButton = new Button("Reset");
        resetButton.setWidthFull();
        resetButton.setIcon(new Icon(VaadinIcon.TRASH));
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.addClickListener(_ -> {
            nameField.clear();
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

        HorizontalLayout header = new HorizontalLayout(nameField, buttonLayout);
        header.setWidthFull();
        header.setAlignSelf(Alignment.END, nameField);

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
        grid.addColumn("uses").setHeader("Uses");
        grid.addColumn("buyPrice").setRenderer(new TextRenderer<>(KeyGridViewRecord::getBuyPriceRouble)).setHeader("Price");
        grid.addColumn("pricePerUse").setRenderer(new TextRenderer<>(KeyGridViewRecord::getPricePerUseRouble)).setHeader("Price Per Use");

        add(grid);
    }

    private void refreshGrid() {
        Condition[] conditions = this.conditions.values().toArray(Condition[]::new);
        Result<KeyGridViewRecord> records = keyGridViewRepository.read(conditions);
        grid.setItems(records);
    }


}
