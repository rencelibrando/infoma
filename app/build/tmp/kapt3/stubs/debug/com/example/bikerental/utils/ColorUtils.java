package com.example.bikerental.utils;

import androidx.compose.runtime.Composable;
import com.example.bikerental.R;

/**
 * Utility object for consistent color usage across the app.
 * This centralizes color definitions to avoid redundancy.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b4\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0015\u0010(\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b)\u0010\u0006J\u0015\u0010*\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b+\u0010\u0006J\u0015\u0010,\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b-\u0010\u0006J\u0015\u0010.\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b/\u0010\u0006J\u0015\u00100\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b1\u0010\u0006J\u0015\u00102\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b3\u0010\u0006J\u0015\u00104\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b5\u0010\u0006J\u0015\u00106\u001a\u00020\u0004H\u0007\u00f8\u0001\u0001\u00f8\u0001\u0000\u00a2\u0006\u0004\b7\u0010\u0006R\u0019\u0010\u0003\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u0005\u0010\u0006R\u0019\u0010\b\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\t\u0010\u0006R\u0019\u0010\n\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u000b\u0010\u0006R\u0019\u0010\f\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\r\u0010\u0006R\u0019\u0010\u000e\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u000f\u0010\u0006R\u0019\u0010\u0010\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u0011\u0010\u0006R\u0019\u0010\u0012\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u0013\u0010\u0006R\u0019\u0010\u0014\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u0015\u0010\u0006R\u0019\u0010\u0016\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u0017\u0010\u0006R\u0019\u0010\u0018\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u0019\u0010\u0006R\u0019\u0010\u001a\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u001b\u0010\u0006R\u0019\u0010\u001c\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u001d\u0010\u0006R\u0019\u0010\u001e\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\u001f\u0010\u0006R\u0019\u0010 \u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b!\u0010\u0006R\u0019\u0010\"\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b#\u0010\u0006R\u0019\u0010$\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b%\u0010\u0006R\u0019\u0010&\u001a\u00020\u0004\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\n\n\u0002\u0010\u0007\u001a\u0004\b\'\u0010\u0006\u0082\u0002\u000b\n\u0005\b\u00a1\u001e0\u0001\n\u0002\b!\u00a8\u00068"}, d2 = {"Lcom/example/bikerental/utils/ColorUtils;", "", "()V", "Amber", "Landroidx/compose/ui/graphics/Color;", "getAmber-0d7_KjU", "()J", "J", "DarkGreen", "getDarkGreen-0d7_KjU", "DarkText", "getDarkText-0d7_KjU", "Error", "getError-0d7_KjU", "Info", "getInfo-0d7_KjU", "LightAmber", "getLightAmber-0d7_KjU", "LightGreen", "getLightGreen-0d7_KjU", "LightText", "getLightText-0d7_KjU", "Pink40", "getPink40-0d7_KjU", "Pink80", "getPink80-0d7_KjU", "Purple40", "getPurple40-0d7_KjU", "Purple80", "getPurple80-0d7_KjU", "PurpleGrey40", "getPurpleGrey40-0d7_KjU", "PurpleGrey80", "getPurpleGrey80-0d7_KjU", "SoftCoral", "getSoftCoral-0d7_KjU", "Success", "getSuccess-0d7_KjU", "Warning", "getWarning-0d7_KjU", "black", "black-0d7_KjU", "blackcol", "blackcol-0d7_KjU", "purple200", "purple200-0d7_KjU", "purple500", "purple500-0d7_KjU", "purple700", "purple700-0d7_KjU", "teal200", "teal200-0d7_KjU", "teal700", "teal700-0d7_KjU", "white", "white-0d7_KjU", "app_debug"})
public final class ColorUtils {
    private static final long DarkGreen = 0L;
    private static final long LightGreen = 0L;
    private static final long Amber = 0L;
    private static final long LightAmber = 0L;
    private static final long SoftCoral = 0L;
    private static final long LightText = 0L;
    private static final long DarkText = 0L;
    private static final long Success = 0L;
    private static final long Warning = 0L;
    private static final long Error = 0L;
    private static final long Info = 0L;
    private static final long Purple80 = 0L;
    private static final long PurpleGrey80 = 0L;
    private static final long Pink80 = 0L;
    private static final long Purple40 = 0L;
    private static final long PurpleGrey40 = 0L;
    private static final long Pink40 = 0L;
    @org.jetbrains.annotations.NotNull()
    public static final com.example.bikerental.utils.ColorUtils INSTANCE = null;
    
    private ColorUtils() {
        super();
    }
}