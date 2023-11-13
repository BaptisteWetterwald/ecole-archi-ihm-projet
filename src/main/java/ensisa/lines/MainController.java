package ensisa.lines;

import ensisa.lines.commands.DeleteCommand;
import ensisa.lines.commands.UndoRedoHistory;
import ensisa.lines.commands.UndoableCommand;
import ensisa.lines.model.Document;
import ensisa.lines.model.LinesEditor;
import ensisa.lines.model.StraightLine;
import ensisa.lines.tools.DrawTool;
import ensisa.lines.tools.SelectTool;
import ensisa.lines.tools.Tool;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;

public class MainController {

    private final Document document;
    private final HashMap<Object, Object> lineControllers;
    @FXML
    public Pane editorPane;
    private LinesEditor linesEditor;
    private final ObjectProperty<Tool> currentTool;
    @FXML
    private RadioButton selectToolButton;
    @FXML
    private RadioButton drawToolButton;
    private final DrawTool drawTool;
    private final SelectTool selectTool;
    private final ObservableSet<StraightLine> selectedLines;

    private final UndoRedoHistory undoRedoHistory;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem deleteMenuItem;
    @FXML
    private TextField lineWidthTextField;
    @FXML
    private ColorPicker colorPicker;

    public MainController() {
        document = new Document();
        lineControllers = new HashMap<>();
        selectTool = new SelectTool(this);
        drawTool = new DrawTool(this);
        currentTool = new SimpleObjectProperty<>(selectTool);
        selectedLines = FXCollections.observableSet();
        undoRedoHistory = new UndoRedoHistory();
    }

    public ObjectProperty<Tool> currentToolProperty() {
        return currentTool;
    }
    public Tool getCurrentTool() {
        return currentTool.get();
    }
    public void setCurrentTool(Tool currentTool) {
        this.currentTool.set(currentTool);
    }

    public Document getDocument() {
        return document;
    }

    public void execute(UndoableCommand command) {
        undoRedoHistory.execute(command);
    }

    @FXML
    private void lineWidthTextFieldAction() {
        try {
            var value = Double.parseDouble(lineWidthTextField.getText());
            if (value >= 1.0) {
                selectedLines.forEach(straightLine -> {
                    straightLine.setStrokeWidth(value);
                });
            }
        } catch (NumberFormatException ex) {
            System.out.println(ex.getMessage());
        }
    }
    @FXML
    private void colorPickerAction() {
        var color = colorPicker.getValue();
        selectedLines.forEach(straightLine -> {
            straightLine.setColor(color);
        });
    }

    @FXML
    private void undoMenuItemAction() {
        undoRedoHistory.undo();
    }
    @FXML
    private void redoMenuItemAction() {
        undoRedoHistory.redo();
    }

    @FXML
    private void quitMenuAction() {
        Platform.exit();
    }

    @FXML
    private void deleteMenuItemAction() {
        undoRedoHistory.execute(new DeleteCommand(this));
    }

    public void initialize() {
        linesEditor = new LinesEditor(editorPane);
        setClipping();
        initializeToolPalette();
        initializeMenus();
        initializeInspector();
        observeDocument();
        observeSelection();
    }


    private String findCommonStrokeWidth() {
        boolean foundOne = false;
        double width = 0.0;
        for (var l : selectedLines) {
            if (!foundOne) {
                width = l.getStrokeWidth();
                foundOne = true;
            } else {
                if (width != l.getStrokeWidth())
                    return "";
            }
        }
        return foundOne ? String.valueOf(width) : "";
    }

    private void initializeInspector() {
        selectedLines.addListener((SetChangeListener<StraightLine>) change -> {
            lineWidthTextField.setText(findCommonStrokeWidth());
            colorPicker.setValue(findCommonColor());
        });
    }

    private Color findCommonColor() {
        boolean foundOne = false;
        Color color = Color.TRANSPARENT;
        for (var l : selectedLines) {
            if (!foundOne) {
                color = l.getColor();
                foundOne = true;
            } else {
                if (!color.equals(l.getColor()))
                    return Color.TRANSPARENT;
            }
        }
        return foundOne ? color : Color.TRANSPARENT;
    }

    private void initializeMenus() {
        undoMenuItem.disableProperty().
                bind(undoRedoHistory.canUndoProperty().not());
        redoMenuItem.disableProperty().
                bind(undoRedoHistory.canRedoProperty().not());
        deleteMenuItem.disableProperty().bind(Bindings.createBooleanBinding(() ->
                selectedLines.isEmpty(), selectedLines));
    }

    private void observeDocument() {
        document.getLines().addListener((ListChangeListener<StraightLine>) c -> {
            while (c.next()) {
                for (StraightLine line : c.getRemoved()) {
                    deselectLine(line);
                    linesEditor.removeLine(line);
                }
                for (StraightLine line : c.getAddedSubList()) {
                    linesEditor.createLine(line);
                }
            }
        });
    }

    public void selectLine(StraightLine line, boolean keepSelection) {
        if (!keepSelection)
            getSelectedLines().clear();
        getSelectedLines().add(line);
    }

    public void deselectLine(StraightLine line) {
        getSelectedLines().remove(line);
    }

    public void deselectAll() {
        getSelectedLines().clear();
    }

    public LinesEditor getLinesEditor() {
        return linesEditor;
    }

    @FXML
    private void mousePressedInEditor(MouseEvent event) {
        getCurrentTool().mousePressed(event);
    }
    @FXML
    private void mouseDraggedInEditor(MouseEvent event) {
        getCurrentTool().mouseDragged(event);
    }
    @FXML
    private void mouseReleasedInEditor(MouseEvent event) {
        getCurrentTool().mouseReleased(event);
    }

    private void setClipping() {
        final Rectangle clip = new Rectangle();
        editorPane.setClip(clip);
        editorPane.layoutBoundsProperty().addListener((v, oldValue, newValue) -> {
            clip.setWidth(newValue.getWidth());
            clip.setHeight(newValue.getHeight());
        });
    }
    @FXML
    private void selectToolAction() {
        setCurrentTool(selectTool);
    }
    @FXML
    private void drawToolAction() {
        setCurrentTool(drawTool);
    }

    @FXML
    private void mouseEntered(MouseEvent event) {
        getCurrentTool().mouseEntered(event);
    }
    @FXML
    void mouseExited(MouseEvent event) {
        getCurrentTool().mouseExited(event);
    }

    private void initializeToolPalette() {
        // Change style class to not paint the round button
        selectToolButton.getStyleClass().remove("radio-button");
        selectToolButton.getStyleClass().add("toggle-button");
        drawToolButton.getStyleClass().remove("radio-button");
        drawToolButton.getStyleClass().add("toggle-button");
    }

    public ObservableSet<StraightLine> getSelectedLines() {
        return selectedLines;
    }

    public StraightLine findLineForPoint(double x, double y) {
        for (var straightLine : getDocument().getLines()) {
            if (linesEditor.isPointInStartSelectionSquare(x, y, straightLine) ||
                    linesEditor.isPointInEndSelectionSquare(x, y, straightLine) ||
                    linesEditor.isPointInLine(x, y, straightLine))
                return straightLine;
        }
        return null;
    }

    private void observeSelection() {
        selectedLines.addListener((SetChangeListener<StraightLine>) change -> {
            if (change.wasRemoved()) {
                linesEditor.deselectLine(change.getElementRemoved());
            }
            if (change.wasAdded()) {
                linesEditor.selectLine(change.getElementAdded());
            }
        });
    }
}
