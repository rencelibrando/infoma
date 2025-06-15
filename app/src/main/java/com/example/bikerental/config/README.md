# API Configuration

This directory contains the API configuration for the Bambike app.

## API Keys

For security reasons, API keys are not included in the repository. You need to add them manually:

### Option 1: Using resource files (Recommended for development)

1. Copy the template file from `app/templates/api_keys_template.xml` to `app/src/main/res/values/api_keys.xml`
2. Add your API keys as string resources:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="google_maps_key">YOUR_ACTUAL_API_KEY</string>
</resources>
```

3. Make sure to add this file to `.gitignore` to prevent committing your API keys

### Option 2: Using BuildConfig (Recommended for production)

1. Add your API keys to `local.properties` (which is already gitignored):

```
MAPS_API_KEY=your_actual_api_key_here
```

2. In your app's `build.gradle.kts` (or `build.gradle`), add:

```kotlin
android {
    defaultConfig {
        // Other config...
        
        // Read from local.properties
        val localProperties = java.util.Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        
        buildConfigField("String", "MAPS_API_KEY", 
            localProperties.getProperty("MAPS_API_KEY", "\"\""))
    }
}
```

3. Then update `ApiConfig.kt` to use BuildConfig:

```kotlin
fun getMapsApiKey(context: Context): String? {
    // First try from BuildConfig
    if (BuildConfig.MAPS_API_KEY.isNotEmpty()) {
        return BuildConfig.MAPS_API_KEY
    }
    
    // Fall back to resources
    return getApiKey(context, "maps_api_key", "google_maps_key")
}
```

## Using the API Configuration

To use an API key in your code, simply call the appropriate method from `ApiConfig`:

```kotlin
val mapsApiKey = ApiConfig.getMapsApiKey(context)
```

This ensures that API keys are managed centrally and not hardcoded throughout the app. 