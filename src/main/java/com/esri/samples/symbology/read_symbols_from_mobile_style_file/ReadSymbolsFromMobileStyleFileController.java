/*
 * Copyright 2019 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.esri.samples.symbology.read_symbols_from_mobile_style_file;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.MultilayerPointSymbol;
import com.esri.arcgisruntime.symbology.MultilayerSymbol;
import com.esri.arcgisruntime.symbology.SymbolStyle;
import com.esri.arcgisruntime.symbology.SymbolStyleSearchParameters;
import com.esri.arcgisruntime.symbology.SymbolStyleSearchResult;

public class ReadSymbolsFromMobileStyleFileController {

  @FXML
  private MapView mapView = new MapView();
  @FXML
  private HBox symbolPreview = new HBox();
  @FXML
  private HBox hatsContainer = new HBox();
  @FXML
  private HBox eyesContainer = new HBox();
  @FXML
  private HBox mouthsContainer = new HBox();

  private GraphicsOverlay graphicsOverlay;
  private MultilayerSymbol currentMultilayerSymbol;
  private SymbolStyle emojiStyle;
  private ArrayList<String> symbolKeys = new ArrayList<>();

  @FXML
  public void initialize() {

    // create a map
    ArcGISMap map = new ArcGISMap(Basemap.createTopographic());
    // add the map to the map view
    mapView.setMap(map);

    // create a graphics overlay and add it to the map
    graphicsOverlay = new GraphicsOverlay();
    mapView.getGraphicsOverlays().add(graphicsOverlay);

    loadSymbolsFromStyleFile();

    // listen to mouse clicks to add the desired multi layer symbol
    mapView.setOnMouseClicked( e -> {
      // convert clicked point to a map point
      Point mapPoint = mapView.screenToLocation(new Point2D(e.getX(), e.getY()));

      // create a new graphic with the point and symbol
      Graphic graphic = new Graphic(mapPoint, currentMultilayerSymbol);
      graphicsOverlay.getGraphics().add(graphic);

    });

  }

  /**
   * Loads the stylx file and searches for all symbols contained within. Put the resulting symbols into the GUI
   * based on their category (eyes, mouth, hat, face).
   */
  private void loadSymbolsFromStyleFile(){
    // create a SymbolStyle by passing the location of the .stylx file in the constructor
    emojiStyle = new SymbolStyle("./samples-data/stylx/emoji-mobile.stylx");
    emojiStyle.loadAsync();

    // add a listener to run when the symbol style has loaded
    emojiStyle.addDoneLoadingListener(() -> {
      if (emojiStyle.getLoadStatus() == LoadStatus.FAILED_TO_LOAD){
        new Alert(Alert.AlertType.ERROR, "Error: could not load .stylx file. Details: "+ emojiStyle.getLoadError().getMessage()).show();
        return;
      }

      // load the default search parameters
      ListenableFuture<SymbolStyleSearchParameters> defaultSearchParametersFuture = emojiStyle.getDefaultSearchParametersAsync();
      defaultSearchParametersFuture.addDoneListener(()->{
        try {
          SymbolStyleSearchParameters defaultSearchParameters = defaultSearchParametersFuture.get();

          // use the default parameters to perform the search
          ListenableFuture<List<SymbolStyleSearchResult>> symbolStyleSearchResultFuture = emojiStyle.searchSymbolsAsync(defaultSearchParameters);
          symbolStyleSearchResultFuture.addDoneListener(()->{
            try {

              // create an empty placeholder image to represent "no symbol" for each category
              ImageView emptyImage = null;

              // create lists to contain the available symbol layers for each category of symbol and add an empty entry as default
              ArrayList<SymbolLayerInfo> eyeSymbolInfos = new ArrayList<>(Collections.singletonList(new SymbolLayerInfo( "","", emptyImage)));
              ArrayList<SymbolLayerInfo> mouthSymbolInfos = new ArrayList<>(Collections.singletonList(new SymbolLayerInfo( "","", emptyImage)));
              ArrayList<SymbolLayerInfo> hatSymbolInfos = new ArrayList<>(Collections.singletonList(new SymbolLayerInfo( "","", emptyImage)));

              // loop through the results and add symbols infos into the list according to category
              List<SymbolStyleSearchResult> symbolStyleSearchResults = symbolStyleSearchResultFuture.get();
              for (SymbolStyleSearchResult symbolStyleSearchResult : symbolStyleSearchResults) {

                // get the symbol for this result
                MultilayerPointSymbol multilayerPointSymbol = (MultilayerPointSymbol) symbolStyleSearchResult.getSymbol();

                // create a swatch image for the symbol (to be used for the preview)
                ListenableFuture<Image> imageListenableFuture = multilayerPointSymbol.createSwatchAsync(0x00000000, 1);
                imageListenableFuture.addDoneListener(()->{
                  try {
                    Image image = imageListenableFuture.get();
                    ImageView imagePreview = new ImageView(image);

                    // create a symbol layer info object to represent the found symbol in the list
                    SymbolLayerInfo symbolLayerInfo = new SymbolLayerInfo(symbolStyleSearchResult.getName(), symbolStyleSearchResult.getKey(), imagePreview);

                    // add the symbol layer info object to the correct list for its category
                    switch (symbolStyleSearchResult.getCategory().toLowerCase()) {
                      case "eyes":
                        eyeSymbolInfos.add(symbolLayerInfo);
                        // add the preview of the symbol to the preview container
                        eyesContainer.getChildren().add(symbolLayerInfo.getImagePreview());
                        break;
                      case "mouth":
                        mouthSymbolInfos.add(symbolLayerInfo);
                        // add the preview of the symbol to the preview container
                        mouthsContainer.getChildren().add(symbolLayerInfo.getImagePreview());
                        break;
                      case "hat":
                        hatSymbolInfos.add(symbolLayerInfo);
                        // add the preview of the symbol to the preview container
                        hatsContainer.getChildren().add(symbolLayerInfo.getImagePreview());
                        break;
                    }

                  } catch (InterruptedException | ExecutionException e){

                  }
                });

              }

            } catch (InterruptedException | ExecutionException e) {
              new Alert(Alert.AlertType.ERROR, "Error performing the symbol search"+ e.getMessage()).show();
            }
          });
        } catch (InterruptedException | ExecutionException e) {
          new Alert(Alert.AlertType.ERROR, "Error retrieving default search parameters for symbol search"+ e.getMessage()).show();
        }
      });
    });
  }

  // a class used to store the information about a symbol layer
  private class SymbolLayerInfo
  {
    // an image view used to preview the symbol
    private ImageView imagePreview;

    // the name of the symbol as it appreas in the mobile style
    private String name;

    // a key that identifies the symbol within the style
    private String key;

    public SymbolLayerInfo(String name, String key, ImageView imagePreview) {
      this.imagePreview = imagePreview;
      this.name = name;
      this.key = key;
    }

    public ImageView getImagePreview() {
      return imagePreview;
    }

    public void setImagePreview(ImageView imagePreview) {
      this.imagePreview = imagePreview;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  public void terminate() {

    if (mapView != null) {
      mapView.dispose();
    }
  }
}