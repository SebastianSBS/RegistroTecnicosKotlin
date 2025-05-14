package edu.ucne.registrotecnicos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import edu.ucne.registrotecnicoskotlin.ui.theme.RegistroTecnicosKotlinTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var tecnicoDb: TecnicoDb
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        tecnicoDb = Room.databaseBuilder(
            applicationContext,
            TecnicoDb::class.java,
            "Tecnico.db"
        ).fallbackToDestructiveMigration()
            .build()

        setContent {
            RegistroTecnicosKotlinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        TecnicoScreen()
                    }

                }
            }
        }
    }


    @Composable
    fun TecnicoScreen() {
        var nombres by remember { mutableStateOf("") }
        var sueldo by remember { mutableStateOf(0.0) }
        var errorMessage: String? by remember { mutableStateOf(null) }

        val scope = rememberCoroutineScope()
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        val tecnicoList by tecnicoDb.tecnicoDao().getAll()
            .collectAsStateWithLifecycle(
                initialValue = emptyList(),
                lifecycleOwner = lifecycleOwner,
                minActiveState = Lifecycle.State.STARTED
            )

        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(8.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        OutlinedTextField(
                            label = { Text(text = "Cliente") },
                            value = nombres,
                            onValueChange = { nombres = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            label = { Text(text = "Sueldo") },
                            value = if (sueldo == 0.0) "" else sueldo.toString(),
                            onValueChange = {
                                sueldo = it.toDoubleOrNull() ?: 0.0
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.padding(2.dp))

                        errorMessage?.let {
                            Text(text = it, color = Color.Red)
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    nombres = ""
                                    sueldo = 0.0
                                    errorMessage = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "new button"
                                )
                                Text(text = "Nuevo")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (nombres.isBlank()) {
                                        errorMessage = "Nombre vacío"
                                        return@OutlinedButton
                                    }

                                    scope.launch {
                                        saveTecnico(
                                            TecnicoEntity(
                                                nombres = nombres,
                                                sueldo = sueldo
                                            )
                                        )
                                        nombres = ""
                                        sueldo = 0.0
                                        errorMessage = null
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "save button"
                                )
                                Text(text = "Guardar")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TecnicoListScreen(tecnicoList)
            }
        }
    }

    @Composable
    fun TecnicoListScreen(tecnicoList: List<TecnicoEntity>) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Lista de técnicos", style = MaterialTheme.typography.titleLarge)

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(tecnicoList) {
                    TecnicoRow(it)
                }
            }
        }
    }

    @Composable
    private fun TecnicoRow(tecnico: TecnicoEntity) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(modifier = Modifier.weight(1f), text = tecnico.tecnicoId?.toString() ?: "")
            Text(
                modifier = Modifier.weight(2f),
                text = tecnico.nombres,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(modifier = Modifier.weight(2f), text = tecnico.sueldo?.toString() ?: "")
        }
        HorizontalDivider()
    }

    private suspend fun saveTecnico(tecnico: TecnicoEntity) {
        tecnicoDb.tecnicoDao().save(tecnico)
    }


    @Entity(tableName = "Tecnicos")
    data class TecnicoEntity(
        @PrimaryKey
        val tecnicoId: Int? = null,
        val nombres: String = "",
        val sueldo: Double? = null
    )


    @Dao
    interface TecnicoDao {
        @Upsert()
        suspend fun save(tecnico: TecnicoEntity)

        @Query(
            """
        SELECT * 
        FROM Tecnicos 
        WHERE tecnicoId=:id  
        LIMIT 1
        """
        )
        suspend fun find(id: Int): TecnicoEntity?

        @Delete
        suspend fun delete(tecnico: TecnicoEntity)

        @Query("SELECT * FROM Tecnicos")
        fun getAll(): Flow<List<TecnicoEntity>>
    }

    @Database(
        entities = [
            TecnicoEntity::class
        ],
        version = 1,
        exportSchema = false
    )
    abstract class TecnicoDb : RoomDatabase() {
        abstract fun tecnicoDao(): TecnicoDao
    }

    @Preview(showBackground = true, showSystemUi = true)
    @Composable
    fun Preview() {
        RegistroTecnicosKotlinTheme {
            val tecnicoList = listOf(
                TecnicoEntity(1, "Marcos Duran", 50000.00),
                TecnicoEntity(2, "Sebastian Suarez", 50000.00)
            )
        }
    }

}