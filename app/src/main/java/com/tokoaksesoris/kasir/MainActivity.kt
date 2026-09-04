package com.tokoaksesoris.kasir

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tokoaksesoris.kasir.data.AppDatabase
import com.tokoaksesoris.kasir.data.Repository
import com.tokoaksesoris.kasir.databinding.ActivityMainBinding
import com.tokoaksesoris.kasir.ui.dashboard.DashboardFragment
import com.tokoaksesoris.kasir.ui.statistik.StatistikFragment
import com.tokoaksesoris.kasir.utils.CsvHelper
import com.tokoaksesoris.kasir.utils.ThemeHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Repository dibagikan ke semua Fragment lewat activity ini (pola sederhana, tanpa DI framework -> tetap ringan)
    val repository: Repository by lazy {
        Repository(AppDatabase.getInstance(applicationContext).appDao())
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { doExport(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { doImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeHelper.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> { showFragment(DashboardFragment()); true }
                R.id.nav_statistik -> { showFragment(StatistikFragment()); true }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.navHostFragment, fragment)
            .commit()
    }

    fun mulaiExportCsv() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        exportLauncher.launch("kasir_aksesoris_export_$timestamp.csv")
    }

    fun mulaiImportCsv() {
        importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain"))
    }

    private fun doExport(uri: Uri) {
        lifecycleScope.launch {
            try {
                CsvHelper.exportToUri(this@MainActivity, uri, repository)
                Toast.makeText(this@MainActivity, "Export berhasil", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Export gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun doImport(uri: Uri) {
        lifecycleScope.launch {
            try {
                val result = CsvHelper.importFromUri(this@MainActivity, uri, repository)
                if (result.error != null) {
                    Toast.makeText(this@MainActivity, result.error, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Import berhasil: ${result.jumlahSesi} sesi, ${result.jumlahItem} item",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Import gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
