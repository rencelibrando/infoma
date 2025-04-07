package com.example.bikerental.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikerental.ui.theme.BikerentalTheme

@Composable
fun BottomNavigationBar(selectedItem: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(
        modifier = Modifier
            .height(100.dp) // Reduced height for a sleeker look
            .fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface, // Theme-based background
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
            label = { Text("Maps") },
            selected = selectedItem == 0,
            onClick = { onItemSelected(0) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = "Bike Listings") },
            label = { Text("Bikes") },
            selected = selectedItem == 1,
            onClick = { onItemSelected(1) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Bookings") },
            label = { Text("Bookings") },
            selected = selectedItem == 2,
            onClick = { onItemSelected(2) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = selectedItem == 3,
            onClick = { onItemSelected(3) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationBarPreview() {
    BikerentalTheme {
        BottomNavigationBar(selectedItem = 0, onItemSelected = {})
    }
} 