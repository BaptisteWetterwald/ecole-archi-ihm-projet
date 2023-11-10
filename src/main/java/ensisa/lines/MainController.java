package ensisa.lines;

import ensisa.lines.model.Document;
import ensisa.lines.model.LinesEditor;
import ensisa.lines.model.StraightLine;
import ensisa.lines.tools.DrawTool;
import ensisa.lines.tools.Tool;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class MainController {

    private final Document document;
    @FXML
    public Pane editorPane;
    private LinesEditor linesEditor;
    private final ObjectProperty<Tool> currentTool;

    public MainController() {
        document = new Document();
        currentTool = new SimpleObjectProperty<>(new DrawTool(this));
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

    @FXML
    private void quitMenuAction() {
        Platform.exit();
    }

    public void initialize() {
        linesEditor = new LinesEditor(editorPane);
        setClipping();
        observeDocument();
        /*StraightLine l = new StraightLine();
        l.setStartX(10);
        l.setStartY(20);
        l.setEndX(300);
        l.setEndY(60);
        document.getLines().add(l);*/
    }

    private void observeDocument() {
        document.getLines().addListener((ListChangeListener<StraightLine>) c -> {
            while (c.next()) {
                // Des lignes ont été supprimées du modèle
                for (StraightLine line : c.getRemoved()) {
                    linesEditor.removeLine(line);
                }
                // Des lignes ont été ajoutées au modèle
                for (StraightLine line : c.getAddedSubList()) {
                    linesEditor.createLine(line);
                }
            }
        });
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
}
