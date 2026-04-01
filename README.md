# ATH Alert

ATH Alert is a simple Android app that tracks a single crypto by CoinGecko coin id and posts a device notification when CoinGecko records a new all-time high for that coin.

## What it does

- Tracks one coin at a time using a CoinGecko coin id like `bitcoin`, `ethereum`, or `solana`
- Stores the last ATH already announced so you do not get repeat alerts
- Lets you run an immediate manual check
- Schedules recurring checks in the background
- Includes a GitHub Actions workflow to build an APK

## Important behavior

- This starter uses **local Android notifications**, not Firebase cloud push.
- It checks on a periodic schedule through WorkManager, so checks are **approximate**, not exact to the second.
- The first successful check initializes the ATH baseline and does not alert for old historical highs.

## Build locally

Open the folder in Android Studio and build the app.

## Build from GitHub Actions

The included workflow builds a debug APK on pushes to `main`, and on tags like `v1.0.0` it also attaches the APK to a GitHub Release.

Suggested flow:

1. Create a new GitHub repo
2. Upload this project
3. Push to `main`
4. Create a release tag like `v1.0.0`
5. Download the APK from the release assets

## Future upgrades you may want

- Multi-coin watchlist
- User-selectable currencies
- Firebase Cloud Messaging for true server-triggered push delivery
- Faster server-side polling with your own backend
- Price charts and ATH history

## API source used in app

The app calls CoinGecko's `/coins/markets` endpoint and reads `current_price` and `ath` from the response.
