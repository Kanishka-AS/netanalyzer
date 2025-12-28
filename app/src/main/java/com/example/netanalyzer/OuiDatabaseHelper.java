package com.example.netanalyzer;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class OuiDatabaseHelper {
    private static final String TAG = "OuiDatabase";
    private static Map<String, String> ouiDatabase = null;

    public static String getVendorFromMac(Context context, String mac) {
        if (mac == null || mac.equals("Unknown") || mac.length() < 8) {
            return "Unknown Vendor";
        }

        // Extract OUI (first 6 characters)
        String oui = mac.replace(":", "").replace("-", "").substring(0, 6).toUpperCase();

        // Initialize database if needed
        if (ouiDatabase == null) {
            loadOuiDatabase(context);
        }

        // Lookup vendor
        String vendor = ouiDatabase.get(oui);
        return vendor != null ? vendor : "Unknown Vendor";
    }

    private static synchronized void loadOuiDatabase(Context context) {
        if (ouiDatabase != null) return;

        ouiDatabase = new HashMap<>();

        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("oui_database.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() > 7 && line.contains("=")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        ouiDatabase.put(parts[0].trim().toUpperCase(), parts[1].trim());
                    }
                }
            }
            reader.close();

            Log.d(TAG, "Loaded " + ouiDatabase.size() + " OUI entries");
        } catch (Exception e) {
            Log.e(TAG, "Failed to load OUI database: " + e.getMessage());
            // Fallback to hardcoded vendors
            loadHardcodedVendors();
        }
    }

    private static void loadHardcodedVendors() {
        // Common vendor OUIs (if database file fails)
        ouiDatabase = new HashMap<>();

        // Apple
        ouiDatabase.put("001C10", "Apple");
        ouiDatabase.put("A4D1D1", "Apple");
        ouiDatabase.put("F0F0F0", "Apple");
        ouiDatabase.put("3C0754", "Apple");
        ouiDatabase.put("001451", "Apple");

        // Samsung
        ouiDatabase.put("0019B9", "Samsung");
        ouiDatabase.put("5C3C27", "Samsung");
        ouiDatabase.put("001D25", "Samsung");
        ouiDatabase.put("001E7D", "Samsung");
        ouiDatabase.put("0023D6", "Samsung");

        // Google
        ouiDatabase.put("001A11", "Google");
        ouiDatabase.put("DC537C", "Google");
        ouiDatabase.put("F46D04", "Google");
        ouiDatabase.put("D850E6", "Google");

        // Microsoft
        ouiDatabase.put("001D0F", "Microsoft");
        ouiDatabase.put("000D3A", "Microsoft");
        ouiDatabase.put("001548", "Microsoft");
        ouiDatabase.put("00248C", "Microsoft");

        // TP-Link
        ouiDatabase.put("C4E984", "TP-Link");
        ouiDatabase.put("001D0F", "TP-Link");
        ouiDatabase.put("50BD5F", "TP-Link");

        // NETGEAR
        ouiDatabase.put("E45F01", "NETGEAR");
        ouiDatabase.put("001E46", "NETGEAR");
        ouiDatabase.put("001B2F", "NETGEAR");

        // Huawei
        ouiDatabase.put("001124", "Huawei");
        ouiDatabase.put("64167F", "Huawei");
        ouiDatabase.put("AC853D", "Huawei");

        // Sony
        ouiDatabase.put("001DE1", "Sony");
        ouiDatabase.put("00E036", "Sony");
        ouiDatabase.put("001A80", "Sony");

        // LG
        ouiDatabase.put("001BFC", "LG Electronics");
        ouiDatabase.put("001F6B", "LG Electronics");

        // Dell
        ouiDatabase.put("0022B0", "Dell");
        ouiDatabase.put("001DE8", "Dell");

        // Lenovo
        ouiDatabase.put("F48C50", "Lenovo");
        ouiDatabase.put("001A6B", "Lenovo");

        // Asus
        ouiDatabase.put("001F5B", "ASUS");
        ouiDatabase.put("001D60", "ASUS");

        // Intel
        ouiDatabase.put("000D67", "Intel");
        ouiDatabase.put("001B21", "Intel");

        // Cisco
        ouiDatabase.put("0016CB", "Cisco");
        ouiDatabase.put("00000C", "Cisco");

        // Xiaomi
        ouiDatabase.put("001175", "Xiaomi");
        ouiDatabase.put("ACF7F3", "Xiaomi");

        // Amazon
        ouiDatabase.put("002486", "Amazon");
        ouiDatabase.put("F0272D", "Amazon");

        // Raspberry Pi
        ouiDatabase.put("B827EB", "Raspberry Pi");

        Log.d(TAG, "Loaded hardcoded OUI database: " + ouiDatabase.size() + " entries");
    }

    public static String guessDeviceType(String vendor, String hostname, String ip) {
        if (vendor == null && hostname == null && ip == null) {
            return "Unknown";
        }

        String vendorLower = (vendor != null) ? vendor.toLowerCase() : "";
        String hostnameLower = (hostname != null) ? hostname.toLowerCase() : "";

        // Check if it's the router/gateway by common IPs
        if (ip != null) {
            if (ip.equals("192.168.0.1") || ip.equals("192.168.1.1") ||
                    ip.equals("192.168.2.1") || ip.equals("10.0.0.1") ||
                    ip.equals("192.168.100.1")) {
                return "Router/Gateway";
            }
        }

        // Motorola devices
        if (vendorLower.contains("motorola")) {
            if (hostnameLower.contains("tab") || hostnameLower.contains("tablet")) {
                return "Motorola Tablet";
            }
            if (hostnameLower.contains("edge") || hostnameLower.contains("g") ||
                    hostnameLower.contains("moto")) {
                return "Motorola Phone";
            }
            return "Motorola Device";
        }

        // Xiaomi ecosystem
        if (vendorLower.contains("xiaomi") || vendorLower.contains("redmi") ||
                vendorLower.contains("poco") || hostnameLower.contains("mi-") ||
                hostnameLower.contains("redmi") || hostnameLower.contains("poco")) {

            if (hostnameLower.contains("laptop") || hostnameLower.contains("notebook") ||
                    hostnameLower.contains("book") || hostnameLower.contains("pc")) {
                return "Xiaomi Laptop";
            }
            if (hostnameLower.contains("tv")) {
                return "Xiaomi Smart TV";
            }
            if (hostnameLower.contains("pad") || hostnameLower.contains("tablet")) {
                return "Xiaomi Tablet";
            }
            if (hostnameLower.contains("watch") || hostnameLower.contains("band")) {
                return "Xiaomi Wearable";
            }
            if (hostnameLower.contains("router")) {
                return "Xiaomi Router";
            }

            if (vendorLower.contains("redmi") || hostnameLower.contains("redmi")) {
                return "Redmi Phone";
            }
            if (vendorLower.contains("poco") || hostnameLower.contains("poco")) {
                return "Poco Phone";
            }
            return "Xiaomi Phone";
        }

        // Other Chinese brands
        if (vendorLower.contains("realme") || hostnameLower.contains("realme")) {
            return "Realme Phone";
        }
        if (vendorLower.contains("oppo") || hostnameLower.contains("oppo")) {
            return "Oppo Phone";
        }
        if (vendorLower.contains("vivo") || hostnameLower.contains("vivo")) {
            return "Vivo Phone";
        }
        if (vendorLower.contains("oneplus") || hostnameLower.contains("oneplus")) {
            return "OnePlus Phone";
        }

        // Apple devices
        if (vendorLower.contains("apple")) {
            if (hostnameLower.contains("iphone")) return "iPhone";
            if (hostnameLower.contains("ipad")) return "iPad";
            if (hostnameLower.contains("mac") || hostnameLower.contains("macbook")) return "MacBook";
            if (hostnameLower.contains("watch")) return "Apple Watch";
            if (hostnameLower.contains("tv")) return "Apple TV";
            return "Apple Device";
        }

        // Samsung
        if (vendorLower.contains("samsung")) {
            if (hostnameLower.contains("tab") || hostnameLower.contains("tablet")) return "Samsung Tablet";
            if (hostnameLower.contains("book")) return "Samsung Laptop";
            if (hostnameLower.contains("tv")) return "Samsung Smart TV";
            if (hostnameLower.contains("watch")) return "Samsung Watch";
            return "Samsung Phone";
        }

        // Google
        if (vendorLower.contains("google")) {
            if (hostnameLower.contains("pixel")) return "Google Pixel Phone";
            if (hostnameLower.contains("chromebook")) return "Chromebook";
            if (hostnameLower.contains("nest")) return "Google Nest";
            return "Google Device";
        }

        // Lenovo (makes Xiaomi laptops)
        if (vendorLower.contains("lenovo")) {
            if (hostnameLower.contains("xiaomi") || hostnameLower.contains("mi")) {
                return "Xiaomi Laptop";
            }
            if (hostnameLower.contains("thinkpad")) {
                return "Lenovo ThinkPad";
            }
            return "Lenovo Laptop";
        }

        // Other laptops
        if (vendorLower.contains("hp") || vendorLower.contains("hewlett")) {
            return "HP Laptop";
        }
        if (vendorLower.contains("dell")) {
            return "Dell Laptop";
        }
        if (vendorLower.contains("acer")) {
            return "Acer Laptop";
        }
        if (vendorLower.contains("asus")) {
            if (hostnameLower.contains("rog")) {
                return "ASUS Gaming Laptop";
            }
            return "ASUS Laptop";
        }

        // Router brands
        if (vendorLower.contains("router") || vendorLower.contains("gateway") ||
                vendorLower.contains("cisco") || vendorLower.contains("tp-link") ||
                vendorLower.contains("netgear") || vendorLower.contains("d-link") ||
                vendorLower.contains("linksys") || vendorLower.contains("tenda") ||
                vendorLower.contains("mercury")) {
            return "Router/Gateway";
        }

        // Hostname-based detection (fallback)
        if (!hostnameLower.isEmpty()) {
            if (hostnameLower.contains("android") || hostnameLower.contains("phone") ||
                    hostnameLower.contains("mobile") || hostnameLower.contains("galaxy")) {
                return "Android Phone";
            }

            if (hostnameLower.contains("pc") || hostnameLower.contains("laptop") ||
                    hostnameLower.contains("desktop") || hostnameLower.contains("computer") ||
                    hostnameLower.contains("notebook") || hostnameLower.contains("thinkpad")) {
                return "Computer/Laptop";
            }

            if (hostnameLower.contains("tv") || hostnameLower.contains("chromecast") ||
                    hostnameLower.contains("firetv") || hostnameLower.contains("roku") ||
                    hostnameLower.contains("smarttv")) {
                return "Smart TV/Streaming";
            }

            if (hostnameLower.contains("printer") || hostnameLower.contains("print")) {
                return "Printer";
            }

            if (hostnameLower.contains("camera") || hostnameLower.contains("security")) {
                return "Camera";
            }

            if (hostnameLower.contains("iot") || hostnameLower.contains("smart")) {
                return "IoT Device";
            }

            // Detect specific models
            if (hostnameLower.contains("moto") || hostnameLower.contains("motorola")) {
                return "Motorola Phone";
            }
            if (hostnameLower.contains("redmi") || hostnameLower.contains("poco")) {
                return "Xiaomi Phone";
            }
        }

        // Default based on vendor
        if (!vendorLower.isEmpty()) {
            if (vendorLower.contains("phone") || vendorLower.contains("mobile")) {
                return "Phone";
            }
            if (vendorLower.contains("laptop") || vendorLower.contains("notebook")) {
                return "Laptop";
            }
            if (vendorLower.contains("router") || vendorLower.contains("gateway")) {
                return "Router";
            }
        }

        return "Network Device";
    }
}