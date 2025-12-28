package com.example.netanalyzer;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    private List<MainActivity.Device> deviceList;
    private Context context;

    public DeviceAdapter(Context context, List<MainActivity.Device> deviceList) {
        this.context = context;
        this.deviceList = deviceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MainActivity.Device device = deviceList.get(position);

        holder.deviceName.setText(device.getDisplayName());
        holder.deviceIp.setText(device.ipAddress);
        holder.deviceMac.setText(device.macAddress);
        holder.deviceVendor.setText(device.vendor);

        // Set device icon based on type
        int iconRes = getDeviceIcon(device.deviceType, device.vendor);
        holder.deviceIcon.setImageResource(iconRes);

        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(context, DeviceDetailsActivity.class);
                intent.putExtra("device", device);

                // Pass gateway info if available
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    String gateway = mainActivity.getGateway();
                    if (gateway != null) {
                        intent.putExtra("gateway", gateway);
                    }
                }

                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    private int getDeviceIcon(String deviceType, String vendor) {
        if (deviceType != null) {
            if (deviceType.contains("Phone")) return android.R.drawable.ic_dialog_email;
            if (deviceType.contains("Laptop") || deviceType.contains("Computer"))
                return android.R.drawable.ic_menu_save;
            if (deviceType.contains("Router") || deviceType.contains("Gateway"))
                return android.R.drawable.ic_menu_upload;
            if (deviceType.contains("Tablet")) return android.R.drawable.ic_menu_gallery;
            if (deviceType.contains("TV")) return android.R.drawable.ic_menu_camera;
        }

        // Fallback to vendor
        if (vendor != null) {
            if (vendor.contains("Apple")) return android.R.drawable.ic_menu_upload;
            if (vendor.contains("Samsung") || vendor.contains("Google"))
                return android.R.drawable.ic_menu_save;
        }

        return android.R.drawable.ic_dialog_info;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView deviceIcon;
        TextView deviceName, deviceIp, deviceMac, deviceVendor;

        ViewHolder(View itemView) {
            super(itemView);
            deviceIcon = itemView.findViewById(R.id.deviceIcon);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceIp = itemView.findViewById(R.id.deviceIp);
            deviceMac = itemView.findViewById(R.id.deviceMac);
            deviceVendor = itemView.findViewById(R.id.deviceVendor);
        }
    }
}