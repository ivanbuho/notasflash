package com.ivanbuho.notasflash;

import android.content.Context;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private EditText campoNota;
    private TextView estadoTexto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(48, 64, 48, 48);

        TextView titulo = new TextView(this);
        titulo.setText("Notas Flash 1.0");
        titulo.setTextSize(22);
        titulo.setGravity(Gravity.CENTER);

        campoNota = new EditText(this);
        campoNota.setMinLines(4);
        campoNota.setGravity(Gravity.TOP | Gravity.START);

        Button botonGuardar = new Button(this);
        botonGuardar.setText("Guardar nota");
        botonGuardar.setTransformationMethod(null);
        botonGuardar.setOnClickListener(v -> guardarNota());

        estadoTexto = new TextView(this);
        estadoTexto.setText("");
        estadoTexto.setTextSize(15);
        estadoTexto.setGravity(Gravity.CENTER);

        layout.addView(titulo);
        layout.addView(campoNota);
        layout.addView(botonGuardar);
        layout.addView(estadoTexto);
        setContentView(layout);
    }

    private void guardarNota() {
        String contenido = campoNota.getText().toString().trim();
        if (contenido.isEmpty()) {
            estadoTexto.setText("Nota vacia");
            return;
        }
        try {
            File carpeta = new File(getExternalFilesDir(null), "notas");
            if (!carpeta.exists()) {
                carpeta.mkdirs();
            }
            File archivo = new File(carpeta, "nota_" + System.currentTimeMillis() + ".txt");
            FileOutputStream fos = new FileOutputStream(archivo);
            fos.write(contenido.getBytes(StandardCharsets.UTF_8));
            fos.close();

            vibrarCorto();
            campoNota.setText("");
            estadoTexto.setText("Guardada: " + archivo.getName());
        } catch (Exception e) {
            estadoTexto.setText("Error al guardar");
        }
    }

    private void vibrarCorto() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
