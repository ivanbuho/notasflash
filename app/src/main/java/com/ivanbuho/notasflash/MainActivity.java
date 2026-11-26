package com.ivanbuho.notasflash;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "notasflash_prefs";
    private static final String KEY_RUTA = "ruta_notas";
    private static final String RUTA_POR_DEFECTO = "notas";

    private EditText campoRuta;
    private EditText campoNota;
    private TextView estadoTexto;
    private TextView visorNota;
    private ListView listaNotas;

    private final List<File> archivosNotas = new ArrayList<>();
    private ArrayAdapter<String> adaptadorNotas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 48, 40, 48);

        TextView titulo = new TextView(this);
        titulo.setText("Notas Flash 1.0.1");
        titulo.setTextSize(22);
        titulo.setGravity(Gravity.CENTER);

        TextView etiquetaRuta = new TextView(this);
        etiquetaRuta.setText("Ruta para guardar (.txt):");
        etiquetaRuta.setTextSize(15);
        etiquetaRuta.setPadding(0, 24, 0, 6);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String rutaGuardada = prefs.getString(KEY_RUTA, RUTA_POR_DEFECTO);

        campoRuta = new EditText(this);
        campoRuta.setSingleLine(true);
        campoRuta.setText(rutaGuardada);

        Button botonAplicarRuta = new Button(this);
        botonAplicarRuta.setText("Usar ruta y ver notas");
        botonAplicarRuta.setTransformationMethod(null);
        botonAplicarRuta.setOnClickListener(v -> {
            guardarPreferenciaRuta();
            cargarNotasGuardadas();
            estadoTexto.setText("Ruta activa: " + obtenerRutaLimpia());
        });

        TextView etiquetaNota = new TextView(this);
        etiquetaNota.setText("Texto de la nota:");
        etiquetaNota.setTextSize(15);
        etiquetaNota.setPadding(0, 24, 0, 6);

        campoNota = new EditText(this);
        campoNota.setMinLines(3);
        campoNota.setGravity(Gravity.TOP | Gravity.START);

        Button botonGuardar = new Button(this);
        botonGuardar.setText("Guardar nota");
        botonGuardar.setTransformationMethod(null);
        botonGuardar.setOnClickListener(v -> guardarNota());

        estadoTexto = new TextView(this);
        estadoTexto.setText("");
        estadoTexto.setTextSize(14);
        estadoTexto.setPadding(0, 12, 0, 12);

        TextView tituloGuardadas = new TextView(this);
        tituloGuardadas.setText("Notas guardadas");
        tituloGuardadas.setTextSize(18);
        tituloGuardadas.setPadding(0, 16, 0, 8);

        adaptadorNotas = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );

        listaNotas = new ListView(this);
        listaNotas.setAdapter(adaptadorNotas);
        listaNotas.setMinimumHeight(360);
        listaNotas.setNestedScrollingEnabled(true);
        listaNotas.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < archivosNotas.size()) {
                mostrarContenidoNota(archivosNotas.get(position));
            }
        });

        TextView etiquetaVisor = new TextView(this);
        etiquetaVisor.setText("Contenido de la nota seleccionada:");
        etiquetaVisor.setTextSize(15);
        etiquetaVisor.setPadding(0, 16, 0, 6);

        visorNota = new TextView(this);
        visorNota.setText("");
        visorNota.setTextSize(14);
        visorNota.setPadding(16, 16, 16, 16);
        visorNota.setVisibility(View.VISIBLE);

        layout.addView(titulo);
        layout.addView(etiquetaRuta);
        layout.addView(campoRuta);
        layout.addView(botonAplicarRuta);
        layout.addView(etiquetaNota);
        layout.addView(campoNota);
        layout.addView(botonGuardar);
        layout.addView(estadoTexto);
        layout.addView(tituloGuardadas);
        layout.addView(listaNotas);
        layout.addView(etiquetaVisor);
        layout.addView(visorNota);

        scroll.addView(layout);
        setContentView(scroll);

        cargarNotasGuardadas();
    }

    private String obtenerRutaLimpia() {
        String raw = campoRuta.getText().toString().trim();
        raw = raw.replaceAll("^/+", "").replaceAll("/+$", "");
        if (raw.isEmpty()) {
            return RUTA_POR_DEFECTO;
        }
        return raw;
    }

    private void guardarPreferenciaRuta() {
        String limpia = obtenerRutaLimpia();
        campoRuta.setText(limpia);
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_RUTA, limpia)
                .apply();
    }

    private File obtenerDirectorioNotas() {
        String rutaRelativa = obtenerRutaLimpia();
        File base = getExternalFilesDir(null);
        if (base == null) {
            base = new File(Environment.getExternalStorageDirectory(), "Android/data/" + getPackageName() + "/files");
        }
        File carpeta = new File(base, rutaRelativa);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        return carpeta;
    }

    private void guardarNota() {
        String contenido = campoNota.getText().toString().trim();
        if (contenido.isEmpty()) {
            estadoTexto.setText("Nota vacia");
            return;
        }
        try {
            guardarPreferenciaRuta();
            File carpeta = obtenerDirectorioNotas();
            String nombreArchivo = "nota_" + System.currentTimeMillis() + ".txt";
            File archivo = new File(carpeta, nombreArchivo);

            FileOutputStream fos = new FileOutputStream(archivo);
            fos.write(contenido.getBytes(StandardCharsets.UTF_8));
            fos.close();

            vibrarCorto();
            campoNota.setText("");
            estadoTexto.setText("Guardada en " + obtenerRutaLimpia() + "/" + nombreArchivo);
            cargarNotasGuardadas();
        } catch (Exception e) {
            estadoTexto.setText("Error al guardar: " + e.getMessage());
        }
    }

    private void cargarNotasGuardadas() {
        archivosNotas.clear();
        List<String> nombres = new ArrayList<>();

        File carpeta = obtenerDirectorioNotas();
        File[] encontrados = carpeta.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));

        if (encontrados != null && encontrados.length > 0) {
            Arrays.sort(encontrados, Comparator.comparingLong(File::lastModified).reversed());
            for (File f : encontrados) {
                archivosNotas.add(f);
                nombres.add(f.getName());
            }
        } else {
            nombres.add("Sin notas en esta ruta");
        }

        adaptadorNotas.clear();
        adaptadorNotas.addAll(nombres);
        adaptadorNotas.notifyDataSetChanged();
    }

    private void mostrarContenidoNota(File archivo) {
        try {
            StringBuilder sb = new StringBuilder();
            FileInputStream fis = new FileInputStream(archivo);
            InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr, 8192);
            String linea;
            while ((linea = br.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(linea);
            }
            br.close();
            isr.close();
            fis.close();
            visorNota.setText(archivo.getName() + "\n\n" + sb.toString());
        } catch (Exception e) {
            visorNota.setText("No se pudo leer " + archivo.getName());
        }
    }

    private void vibrarCorto() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
