package com.atakmap.android.isproutes.plugin;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import com.atak.plugins.impl.PluginContextProvider;
import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import java.util.UUID;

import gov.tak.api.plugin.IPlugin;
import gov.tak.api.plugin.IServiceController;
import gov.tak.api.ui.IHostUIService;
import gov.tak.api.ui.Pane;
import gov.tak.api.ui.PaneBuilder;
import gov.tak.api.ui.ToolbarItem;
import gov.tak.api.ui.ToolbarItemAdapter;
import gov.tak.platform.marshal.MarshalManager;

public class ISPRoutesPlugin implements IPlugin {

    private final IServiceController serviceController;
    private Context pluginContext;
    private IHostUIService uiService;
    private ToolbarItem toolbarItem;
    private Pane ispRoutesPane;
    private View ispRoutesView;

    private static final RouteMarkerType[] MARKER_TYPES = {
            new RouteMarkerType(R.id.ispPoliceStation, "Police Station",
                    "police_station", R.drawable.ic_isp_police, Color.rgb(33, 150, 243)),
            new RouteMarkerType(R.id.ispHospital, "Hospital",
                    "hospital", R.drawable.ic_isp_hospital, Color.rgb(229, 57, 53)),
            new RouteMarkerType(R.id.ispGasStation, "Gas Station",
                    "gas_station", R.drawable.ic_isp_gas, Color.rgb(251, 192, 45)),
            new RouteMarkerType(R.id.ispSchool, "School",
                    "school", R.drawable.ic_isp_school, Color.rgb(67, 160, 71)),
            new RouteMarkerType(R.id.ispLoi, "LOI",
                    "loi", R.drawable.ic_isp_loi, Color.rgb(0, 188, 212)),
            new RouteMarkerType(R.id.ispCnpLeft, "CNP - Left Turn",
                    "cnp_left_turn", R.drawable.ic_isp_left_turn, Color.rgb(171, 71, 188)),
            new RouteMarkerType(R.id.ispCnpRight, "CNP - Right Turn",
                    "cnp_right_turn", R.drawable.ic_isp_right_turn, Color.rgb(255, 112, 67)),
            new RouteMarkerType(R.id.ispCnpOther, "CNP - Other",
                    "cnp_other", R.drawable.ic_isp_other, Color.rgb(158, 158, 158)),
            new RouteMarkerType(R.id.ispRestricted, "Restricted",
                    "restricted", R.drawable.ic_isp_restricted, Color.rgb(211, 47, 47)),
            new RouteMarkerType(R.id.ispCheckPoint, "Check Point",
                    "check_point", R.drawable.ic_isp_checkpoint, Color.rgb(30, 136, 229)),
            new RouteMarkerType(R.id.ispTunnel, "Tunnel",
                    "tunnel", R.drawable.ic_isp_tunnel, Color.rgb(84, 110, 122)),
            new RouteMarkerType(R.id.ispOverpass, "Overpass",
                    "overpass", R.drawable.ic_isp_overpass, Color.rgb(0, 137, 123))
    };

    public ISPRoutesPlugin(IServiceController serviceController) {
        this.serviceController = serviceController;
        final PluginContextProvider ctxProvider = serviceController
                .getService(PluginContextProvider.class);
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext();
            pluginContext.setTheme(R.style.ATAKPluginTheme);
        }

        uiService = serviceController.getService(IHostUIService.class);

        toolbarItem = new ToolbarItem.Builder(
                pluginContext.getString(R.string.app_name),
                MarshalManager.marshal(
                        pluginContext.getResources().getDrawable(R.drawable.ic_launcher),
                        android.graphics.drawable.Drawable.class,
                        gov.tak.api.commons.graphics.Bitmap.class))
                .setListener(new ToolbarItemAdapter() {
                    @Override
                    public void onClick(ToolbarItem item) {
                        showPane();
                    }
                })
                .build();
    }

    @Override
    public void onStart() {
        if (uiService == null)
            return;

        uiService.addToolbarItem(toolbarItem);
    }

    @Override
    public void onStop() {
        if (uiService == null)
            return;

        uiService.removeToolbarItem(toolbarItem);
        if (ispRoutesPane != null && uiService.isPaneVisible(ispRoutesPane))
            uiService.closePane(ispRoutesPane);
    }

    private void showPane() {
        if (ispRoutesPane == null) {
            ispRoutesView = PluginLayoutInflater.inflate(pluginContext,
                    R.layout.main_layout, null);
            bindMarkerTiles(ispRoutesView);

            ispRoutesPane = new PaneBuilder(ispRoutesView)
                    .setMetaValue(Pane.PANE_NAME,
                            pluginContext.getString(R.string.app_name))
                    .setMetaValue(Pane.RELATIVE_LOCATION, Pane.Location.Right)
                    .setMetaValue(Pane.PREFERRED_WIDTH_RATIO, 0.30D)
                    .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.50D)
                    .build();
        }

        if (!uiService.isPaneVisible(ispRoutesPane)) {
            uiService.showPane(ispRoutesPane, null);
        }
    }

    private void bindMarkerTiles(View root) {
        for (RouteMarkerType markerType : MARKER_TYPES) {
            View tile = root.findViewById(markerType.viewId);
            if (tile != null) {
                tile.setOnTouchListener(new MarkerDragTouchListener(markerType));
            }
        }
    }

    private boolean placeMarkerFromRawCoordinates(RouteMarkerType markerType,
            float rawX, float rawY) {
        MapView mapView = MapView.getMapView();
        if (mapView == null) {
            showToast("Map is not ready");
            return false;
        }

        int[] mapLocation = new int[2];
        mapView.getLocationOnScreen(mapLocation);
        Rect mapBounds = new Rect(mapLocation[0], mapLocation[1],
                mapLocation[0] + mapView.getWidth(),
                mapLocation[1] + mapView.getHeight());

        int screenX = Math.round(rawX);
        int screenY = Math.round(rawY);
        if (!mapBounds.contains(screenX, screenY))
            return false;

        float mapX = rawX - mapLocation[0];
        float mapY = rawY - mapLocation[1];
        GeoPointMetaData point = mapView.inverseWithElevation(mapX, mapY);
        if (point == null || point.get() == null || !point.get().isValid()) {
            showToast("Unable to place marker at that location");
            return false;
        }

        String uid = "isp-routes-" + markerType.key + "-"
                + UUID.randomUUID();
        Marker marker = new PlacePointTool.MarkerCreator(point)
                .setUid(uid)
                .setCallsign(markerType.title)
                .setType("a-u-G")
                .setHow("h-g-i-g-o")
                .setColor(markerType.color)
                .placePoint();

        if (marker == null) {
            showToast("Unable to create marker");
            return false;
        }

        marker.setTitle(markerType.title);
        marker.setMetaString("callsign", markerType.title);
        marker.setMetaString("isp_routes_type", markerType.key);
        marker.setMetaString("isp_routes_label", markerType.title);
        marker.setMetaBoolean("adapt_marker_icon", false);
        marker.setIcon(new Icon.Builder()
                .setImageUri(0, markerType.getIconUri(pluginContext))
                .setColor(0, Color.WHITE)
                .setSize(48, 48)
                .setAnchor(24, 24)
                .build());
        marker.persist(mapView.getMapEventDispatcher(), null,
                ISPRoutesPlugin.class);
        marker.refresh(mapView.getMapEventDispatcher(), null,
                ISPRoutesPlugin.class);

        showToast(markerType.title + " placed");
        return true;
    }

    private void showToast(String message) {
        if (uiService != null)
            uiService.showToast(message);
    }

    private final class MarkerDragTouchListener implements View.OnTouchListener {
        private final RouteMarkerType markerType;

        private MarkerDragTouchListener(RouteMarkerType markerType) {
            this.markerType = markerType;
        }

        @Override
        public boolean onTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.setPressed(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    return true;
                case MotionEvent.ACTION_UP:
                    view.setPressed(false);
                    placeMarkerFromRawCoordinates(markerType,
                            event.getRawX(), event.getRawY());
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    view.setPressed(false);
                    return true;
                default:
                    return false;
            }
        }
    }

    private static final class RouteMarkerType {
        private final int viewId;
        private final String title;
        private final String key;
        private final int iconResId;
        private final int color;

        private RouteMarkerType(int viewId, String title, String key,
                int iconResId, int color) {
            this.viewId = viewId;
            this.title = title;
            this.key = key;
            this.iconResId = iconResId;
            this.color = color;
        }

        private String getIconUri(Context context) {
            return "android.resource://" + context.getPackageName()
                    + "/" + iconResId;
        }
    }
}
