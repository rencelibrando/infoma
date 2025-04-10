# Navigation Components

This directory contains components for the app's navigation system. The app uses a navigation drawer pattern for main app navigation.

## Components Overview

### AppNavigationDrawer
- Implements a side drawer (sidebar) that slides in from the left edge of the screen
- Contains navigation items for the main app sections (Maps, Bikes, Bookings, Profile)
- Features a modern UI design with:
  - User profile picture and information at the top
  - Gradient background header section
  - "Bambike Ecotours" company branding at the bottom
- Replaces the previous bottom navigation bar approach for a more modern UI experience

### AppTopBar
- Transparent top app bar with menu button only (no title text)
- Menu button toggles the navigation drawer
- Clean minimalist design that focuses on content

### SwipeToOpenDrawer
- Provides gesture support to open the drawer by swiping from the left edge of the screen
- Implemented as a Modifier extension function
- Works in conjunction with the AppNavigationDrawer

## Usage

The navigation drawer is implemented in the HomeScreen component. It can be opened in two ways:

1. By tapping the menu icon in the top app bar
2. By swiping from the left edge of the screen

When a navigation item is selected, the drawer automatically closes and the corresponding screen content is displayed.

## Design Considerations

- The drawer width is set to 75% of the screen width for optimal usability
- User profile is prominently displayed at the top of the drawer for personalization
- Gesture detection is optimized for natural interaction
- Navigation drawer items use the same icons and order as the previous bottom navigation to maintain familiarity for users
- Company branding is displayed at the bottom of the drawer for brand reinforcement 