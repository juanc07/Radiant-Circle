package com.thinkblox.radiantrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.thinkblox.radiantrush.solana.MobileWalletRepository
import com.thinkblox.radiantrush.ui.RadiantRushApp
import com.thinkblox.radiantrush.ui.theme.RadiantRushTheme

class MainActivity : ComponentActivity() {
    private lateinit var walletRepository: MobileWalletRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ActivityResultSender registers an Activity Result launcher internally.
        // Android requires that registration to happen before the Activity reaches STARTED.
        // Keep this outside Compose so recomposition cannot create/register it after RESUMED.
        walletRepository = MobileWalletRepository(this)

        enableEdgeToEdge()
        setContent {
            RadiantRushTheme {
                RadiantRushApp(walletRepository = walletRepository)
            }
        }
    }
}
