package csi.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import csi.clock.ui.theme.CSIClockTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.res.Configuration // Necessario per Configuration.ORIENTATION_LANDSCAPE etc.
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import android.content.Context
import android.media.MediaPlayer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.Calendar // Per ottenere i secondi in modo più affidabile


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Abilita il disegno edge-to-edge
        setContent {
            CSIClockTheme {
                // Surface principale dell'applicazione
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White// Colore di sfondo dal tema
                ) {
                    MainScreenLayout()
                }
            }
        }
    }
}

@Composable
fun MainScreenLayout() {
    val configuration = LocalConfiguration.current
    // Ottieni il padding della status bar
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()

    // Determina il padding superiore aggiuntivo solo per l'orientamento verticale
    val topImagePadding = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        statusBarPadding.calculateTopPadding() + 30.dp // Aggiungi l'altezza della status bar + un po' di spazio
    } else {
        16.dp // Padding standard per l'orientamento orizzontale (o se non vuoi padding aggiuntivo)
    }

    // Altezza dell'immagine condizionale
    val imageHeight: Dp = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        200.dp // Altezza maggiore per l'orientamento verticale
    } else {
        150.dp // Altezza standard (o minore) per l'orientamento orizzontale
    }

    Column(
        modifier = Modifier.fillMaxSize()
        // horizontalAlignment = Alignment.CenterHorizontally // Centra l'immagine se non è fillMaxWidth
    ) {
        // Immagine in alto
        Image(
            painter = painterResource(id = R.drawable.csi), // Assicurati che csi.png esista in res/drawable
            contentDescription = "Logo CSI", // Descrizione per l'accessibilità
            modifier = Modifier
                .fillMaxWidth() // Immagine a tutta larghezza
                .height(imageHeight) // Altezza desiderata per l'immagine, puoi aggiustarla
                .padding(top = topImagePadding, start = 16.dp, end = 16.dp), // Aggiunge padding attorno all'immagine se necessario
            contentScale = ContentScale.Fit // Scala l'immagine per adattarsi mantenendo le proporzioni
        )

        // Box per riempire lo spazio rimanente e centrare l'orologio al suo interno
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Fa sì che questo Box prenda tutto lo spazio verticale rimanente
            contentAlignment = Alignment.Center // Centra il contenuto del Box (l'orologio)
        ) {
            ClockView() // Chiama la composable che mostra l'orologio
        }
    }
}

@Composable
fun ClockView(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(getCurrentFormattedTime()) }
    var currentSeconds by remember { mutableStateOf(Calendar.getInstance().get(Calendar.SECOND)) } // Tieni traccia dei secondi

    val context = LocalContext.current
    val mediaPlayer = remember { // Crea e ricorda MediaPlayer
        MediaPlayer.create(context, R.raw.beep) // <<< ASSICURATI DI AVERE beep_sound.mp3 in res/raw
    }

    val mediaPlayer_short = remember { // Crea e ricorda MediaPlayer
        MediaPlayer.create(context, R.raw.beep_short) // <<< ASSICURATI DI AVERE beep_sound.mp3 in res/raw
    }

    // Gestisci il ciclo di vita di MediaPlayer per rilasciare le risorse
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer_short?.release()
        }
    }    // Ottieni la configurazione corrente per determinare l'orientamento
    val configuration = LocalConfiguration.current

    // Determina la dimensione del font in base all'orientamento
    val fontSize = when (configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> 150.sp // Dimensione per orizzontale
        else -> 70.sp // Dimensione per verticale (o default)
    }

    // LaunchedEffect per aggiornare l'ora a intervalli regolari
    // Si avvia una sola volta (key1 = true) e si cancella quando la Composable esce dalla Composizione.
    LaunchedEffect(key1 = true) {
        var lastPlayedSecond = -1 // Per evitare di riprodurre il suono più volte nello stesso secondo
        var lastPlayedMinute = -1 // Per evitare di riprodurre il suono più volte nello stesso minuto

        while (true) {
            val now = Calendar.getInstance()
            val newMinute = now.get(Calendar.MINUTE)
            val newSeconds = now.get(Calendar.SECOND)
            val newMillis = now.get(Calendar.MILLISECOND) // Per un delay più preciso

            currentTime = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(now.time)
            currentSeconds = newSeconds

            // Controlla se i secondi sono cambiati e se è uno dei secondi target
            if (newMinute != lastPlayedMinute) {
                if (newMinute % 10 == 0) { // Controlla se i secondi sono 0, 10, 20, 30, 40, 50
                    mediaPlayer?.start() // Riproduci il suono
                }
                lastPlayedMinute = newMinute
            }

            if (newSeconds != lastPlayedSecond) {
                if (newSeconds == 50 || newSeconds == 55 || newSeconds == 56 || newSeconds == 57 || newSeconds == 58 || newSeconds == 59) {
                    if(newMinute == 9 || newMinute == 19 || newMinute == 29 || newMinute == 39 || newMinute == 49 || newMinute == 59)
                        mediaPlayer_short?.start() // Riproduci il suono
                }
                lastPlayedSecond = newSeconds
            }

            // Calcola il delay per il prossimo aggiornamento.
            // Aggiorna più frequentemente per i millisecondi, ma controlla il suono solo al cambio di secondo.
            val delayMillis = if (newMillis < 990) 10L else (1000L - newMillis + 10L) // Aspetta fino al prossimo tick utile
            delay(delayMillis)
        }
    }

    // Testo che visualizza l'ora
    Text(
        text = currentTime,
        color = Color.Black, // Colore del testo
        fontSize = fontSize, // Dimensione del testo basata sull'orientamento
        modifier = modifier // Applica eventuali modificatori passati dall'esterno
    )
}

// Funzione helper per ottenere l'ora formattata corrente
private fun getCurrentFormattedTime(): String {
    // Formato: HH (ore 0-23), mm (minuti), ss (secondi), SSS (millisecondi)
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date())
}

// Anteprima per l'orientamento verticale
@Preview(name = "Main Screen Portrait", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
fun MainScreenPortraitPreview() {
    CSIClockTheme {
        Surface(color = Color.White) { // Aggiungi Surface per coerenza con l'app
            MainScreenLayout()
        }
    }
}

// Anteprima per l'orientamento orizzontale
@Preview(name = "Main Screen Landscape", showBackground = true, widthDp = 780, heightDp = 360)
@Composable
fun MainScreenLandscapePreview() {
    CSIClockTheme {
        Surface(color = Color.White) { // Aggiungi Surface per coerenza con l'app
            MainScreenLayout()
        }
    }
}
