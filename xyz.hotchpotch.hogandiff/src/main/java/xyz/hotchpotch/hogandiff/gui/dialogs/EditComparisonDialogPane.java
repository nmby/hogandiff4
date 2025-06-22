package xyz.hotchpotch.hogandiff.gui.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import xyz.hotchpotch.hogandiff.AppMain;
import xyz.hotchpotch.hogandiff.models.BookInfo;
import xyz.hotchpotch.hogandiff.models.DirInfo;
import xyz.hotchpotch.hogandiff.models.PairingInfo;
import xyz.hotchpotch.hogandiff.util.Pair;
import xyz.hotchpotch.hogandiff.util.Pair.Side;

/*package*/ abstract class EditComparisonDialogPane<T extends PairingInfo> extends VBox {

    // [static members] ********************************************************

    protected enum ItemType {

        // static members ------------------------------------------------------

        /** フォルダ */
        DIR("icon-folder.png"),

        /** Excelブック */
        BOOK("icon-book.png"),

        /** シート */
        SHEET("icon-sheet.png");

        public static ItemType of(Object item) {
            if (item instanceof DirInfo) {
                return DIR;

            } else if (item instanceof BookInfo) {
                return BOOK;

            } else if (item instanceof String) {
                return SHEET;
            }
            throw new IllegalArgumentException();
        }

        // instance members ----------------------------------------------------

        public final Image image;

        private ItemType(String imagePath) {
            this.image = new Image(ItemType.class.getResourceAsStream(imagePath));
        }

        public ImageView createImageView(double size) {
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(size);
            return imageView;
        }
    }

    // [instance members] ******************************************************

    protected ResourceBundle rb = AppMain.appResource.get();

    @FXML
    protected GridPane parentGridPane;

    @FXML
    protected Label parentLabelA;

    @FXML
    protected Label parentLabelB;

    @FXML
    protected GridPane childGridPane;

    protected final List<Pair<?>> currentChildPairs = new ArrayList<>();

    /**
     * コンストラクタ<br>
     * 
     * @throws IOException FXMLファイルの読み込みに失敗した場合
     */
    public EditComparisonDialogPane() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("EditComparisonDialogPane.fxml"), rb);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();
    }

    /**
     * このダイアログボックス要素を初期化します。<br>
     * 
     * @param parentType 比較対象親要素の型
     * @param parentPair 比較対象親要素
     * @param childPairs 比較対象子要素
     */
    /* package */ void init(Pair<?> parentPair) throws IOException {

        // コンテンツの長さが異なると均等にサイジングされないため、わざわざBindingとして実装することにする
        childGridPane.widthProperty().addListener((target, oldValue, newValue) -> {
            double colWidth = (newValue.doubleValue() - 50) / 2;
            parentGridPane.getColumnConstraints().get(0).setPrefWidth(colWidth);
            parentGridPane.getColumnConstraints().get(2).setPrefWidth(colWidth);
            childGridPane.getColumnConstraints().get(0).setPrefWidth(colWidth);
            childGridPane.getColumnConstraints().get(2).setPrefWidth(colWidth);
        });

        ItemType itemType = ItemType.of(parentPair.a());
        parentLabelA.setGraphic(itemType.createImageView(24));
        parentLabelB.setGraphic(itemType.createImageView(24));
        parentLabelA.setText("【A】 " + parentPair.a().toString());
        parentLabelB.setText("【B】 " + parentPair.b().toString());
    }

    protected void drawGrid() {
        childGridPane.getChildren().clear();

        for (int i = 0; i < currentChildPairs.size(); i++) {
            Pair<?> pair = currentChildPairs.get(i);
            GridRow gridRow = new GridRow(this, i, pair);

            childGridPane.add(gridRow, 0, i, 3, 1);
            childGridPane.add(gridRow.itemPair().a(), 0, i);
            childGridPane.add(gridRow.itemPair().b(), 2, i);
            if (pair.isPaired()) {
                childGridPane.add(gridRow.unpairButton(), 1, i);
            }

            GridPane.setMargin(gridRow.itemPair().a(), new Insets(2, 3, 2, 3));
            GridPane.setMargin(gridRow.itemPair().b(), new Insets(2, 3, 2, 3));
        }
    }

    public abstract T getResult();

    protected abstract void unpair(int i);

    protected abstract void makePair(int src, int dst);

    protected void onClickPaired(int i) {
    }

    protected void onPasswordChallenge(int idx, Side side) {
    }
}
