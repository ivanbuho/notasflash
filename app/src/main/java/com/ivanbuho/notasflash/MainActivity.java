package com.ivanbuho.notasflash;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.DocumentsContract;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "notasflash_prefs";
    private static final String KEY_TREE_URI = "tree_uri";
    private static final String SUBCARPETA_DEFECTO = "notas";
    private static final String VERSION_APP = "1.0.2";
    private static final String CREADOR_APP = "IvanBuho";

    private LinearLayout panelInicio;
    private LinearLayout panelGuardados;
    private LinearLayout panelAjustes;

    private EditText campoTitulo;
    private EditText campoCuerpo;
    private TextView estadoInicio;

    private ListView listaNotas;
    private TextView visorLectura;
    private TextView estadoGuardados;

    private TextView textoRutaActual;
    private TextView estadoAjustes;

    private DocumentFile archivoEditandoDoc;
    private File archivoEditandoFile;

    private final List<EntradaNota> entradasNotas = new ArrayList<>();
    private ArrayAdapter<String> adaptadorNotas;

    private final ActivityResultLauncher<Intent> selectorCarpetaLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        int flags = result.getData().getFlags()
                                & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        if (flags == 0) {
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        }
                        try {
                            getContentResolver().takePersistableUriPermission(treeUri, flags);
                        } catch (Exception ignored) {
                        }
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                .edit()
                                .putString(KEY_TREE_URI, treeUri.toString())
                                .apply();
                        actualizarTextoRuta();
                        estadoAjustes.setText("Ruta actualizada");
                        vibrarCorto();
                    }
                }
            });

    private static class EntradaNota {
        final String nombre;
        final DocumentFile docFile;
        final File localFile;

        EntradaNota(String nombre, DocumentFile docFile, File localFile) {
            this.nombre = nombre;
            this.docFile = docFile;
            this.localFile = localFile;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout raiz = new LinearLayout(this);
        raiz.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollContenido = new ScrollView(this);
        scrollContenido.setFillViewport(true);

        LinearLayout contenedorSecciones = new LinearLayout(this);
        contenedorSecciones.setOrientation(LinearLayout.VERTICAL);
        contenedorSecciones.setPadding(40, 48, 40, 24);

        construirPanelInicio();
        construirPanelGuardados();
        construirPanelAjustes();

        contenedorSecciones.addView(panelInicio);
        contenedorSecciones.addView(panelGuardados);
        contenedorSecciones.addView(panelAjustes);

        scrollContenido.addView(contenedorSecciones);

        LinearLayout.LayoutParams paramsScroll = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        raiz.addView(scrollContenido, paramsScroll);
        raiz.addView(construirBarraInferior());

        setContentView(raiz);
        mostrarSeccion("inicio");
    }

    private void construirPanelInicio() {
        panelInicio = new LinearLayout(this);
        panelInicio.setOrientation(LinearLayout.VERTICAL);

        TextView nombreApp = new TextView(this);
        nombreApp.setText("Notas Flash");
        nombreApp.setTextSize(22);
        nombreApp.setGravity(Gravity.CENTER);
        nombreApp.setPadding(0, 0, 0, 28);

        campoTitulo = new EditText(this);
        campoTitulo.setSingleLine(true);

        campoCuerpo = new EditText(this);
        campoCuerpo.setMinLines(6);
        campoCuerpo.setGravity(Gravity.TOP | Gravity.START);

        Button botonGuardar = new Button(this);
        botonGuardar.setText("Guardar nota");
        botonGuardar.setTransformationMethod(null);
        botonGuardar.setOnClickListener(v -> guardarNota());

        estadoInicio = new TextView(this);
        estadoInicio.setText("");
        estadoInicio.setTextSize(14);
        estadoInicio.setPadding(0, 12, 0, 0);

        panelInicio.addView(nombreApp);
        panelInicio.addView(campoTitulo);
        panelInicio.addView(campoCuerpo);
        panelInicio.addView(botonGuardar);
        panelInicio.addView(estadoInicio);
    }

    private void construirPanelGuardados() {
        panelGuardados = new LinearLayout(this);
        panelGuardados.setOrientation(LinearLayout.VERTICAL);

        TextView tituloGuardados = new TextView(this);
        tituloGuardados.setText("Guardados");
        tituloGuardados.setTextSize(20);
        tituloGuardados.setPadding(0, 0, 0, 12);

        estadoGuardados = new TextView(this);
        estadoGuardados.setText("");
        estadoGuardados.setTextSize(14);
        estadoGuardados.setPadding(0, 0, 0, 8);

        adaptadorNotas = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );

        listaNotas = new ListView(this);
        listaNotas.setAdapter(adaptadorNotas);
        listaNotas.setMinimumHeight(340);
        listaNotas.setNestedScrollingEnabled(true);

        listaNotas.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < entradasNotas.size()) {
                abrirModoLectura(entradasNotas.get(position));
            }
        });

        listaNotas.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < entradasNotas.size()) {
                mostrarOpcionesNota(entradasNotas.get(position));
                return true;
            }
            return false;
        });

        visorLectura = new TextView(this);
        visorLectura.setText("");
        visorLectura.setTextSize(15);
        visorLectura.setPadding(16, 20, 16, 16);

        panelGuardados.addView(tituloGuardados);
        panelGuardados.addView(estadoGuardados);
        panelGuardados.addView(listaNotas);
        panelGuardados.addView(visorLectura);
    }

    private void construirPanelAjustes() {
        panelAjustes = new LinearLayout(this);
        panelAjustes.setOrientation(LinearLayout.VERTICAL);

        TextView tituloAjustes = new TextView(this);
        tituloAjustes.setText("Ajustes");
        tituloAjustes.setTextSize(20);
        tituloAjustes.setPadding(0, 0, 0, 16);

        textoRutaActual = new TextView(this);
        textoRutaActual.setTextSize(14);
        textoRutaActual.setPadding(0, 0, 0, 12);

        Button botonSeleccionarRuta = new Button(this);
        botonSeleccionarRuta.setText("Seleccionar ruta");
        botonSeleccionarRuta.setTransformationMethod(null);
        botonSeleccionarRuta.setOnClickListener(v -> abrirSelectorCarpeta());

        estadoAjustes = new TextView(this);
        estadoAjustes.setText("");
        estadoAjustes.setTextSize(14);
        estadoAjustes.setPadding(0, 12, 0, 24);

        TextView infoVersion = new TextView(this);
        infoVersion.setText("Versión: " + VERSION_APP);
        infoVersion.setTextSize(15);
        infoVersion.setPadding(0, 0, 0, 8);

        TextView infoCreador = new TextView(this);
        infoCreador.setText("Creador: " + CREADOR_APP);
        infoCreador.setTextSize(15);

        panelAjustes.addView(tituloAjustes);
        panelAjustes.addView(textoRutaActual);
        panelAjustes.addView(botonSeleccionarRuta);
        panelAjustes.addView(estadoAjustes);
        panelAjustes.addView(infoVersion);
        panelAjustes.addView(infoCreador);

        actualizarTextoRuta();
    }

    private LinearLayout construirBarraInferior() {
        LinearLayout barra = new LinearLayout(this);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.CENTER);
        barra.setPadding(16, 12, 16, 16);

        Button btnInicio = new Button(this);
        btnInicio.setText("Inicio");
        btnInicio.setTransformationMethod(null);
        btnInicio.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnInicio.setOnClickListener(v -> mostrarSeccion("inicio"));

        Button btnGuardados = new Button(this);
        btnGuardados.setText("Guardados");
        btnGuardados.setTransformationMethod(null);
        btnGuardados.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnGuardados.setOnClickListener(v -> mostrarSeccion("guardados"));

        Button btnAjustes = new Button(this);
        btnAjustes.setText("Ajustes");
        btnAjustes.setTransformationMethod(null);
        btnAjustes.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        btnAjustes.setOnClickListener(v -> mostrarSeccion("ajustes"));

        barra.addView(btnInicio);
        barra.addView(btnGuardados);
        barra.addView(btnAjustes);
        return barra;
    }

    private void mostrarSeccion(String seccion) {
        panelInicio.setVisibility("inicio".equals(seccion) ? View.VISIBLE : View.GONE);
        panelGuardados.setVisibility("guardados".equals(seccion) ? View.VISIBLE : View.GONE);
        panelAjustes.setVisibility("ajustes".equals(seccion) ? View.VISIBLE : View.GONE);

        if ("guardados".equals(seccion)) {
            visorLectura.setText("");
            cargarNotasGuardadas();
        } else if ("ajustes".equals(seccion)) {
            actualizarTextoRuta();
        }
    }

    private void abrirSelectorCarpeta() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        Uri uriGuardada = obtenerUriGuardada();
        if (uriGuardada != null) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriGuardada);
        }
        try {
            selectorCarpetaLauncher.launch(intent);
        } catch (Exception e) {
            estadoAjustes.setText("No se pudo abrir el selector de carpetas");
        }
    }

    private Uri obtenerUriGuardada() {
        String uriStr = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getString(KEY_TREE_URI, null);
        if (uriStr == null || uriStr.trim().isEmpty()) {
            return null;
        }
        return Uri.parse(uriStr);
    }

    private DocumentFile obtenerCarpetaDocumento() {
        Uri uri = obtenerUriGuardada();
        if (uri == null) {
            return null;
        }
        return DocumentFile.fromTreeUri(this, uri);
    }

    private File obtenerCarpetaLocalDefecto() {
        File base = getExternalFilesDir(null);
        if (base == null) {
            base = new File(Environment.getExternalStorageDirectory(), "Android/data/" + getPackageName() + "/files");
        }
        File carpeta = new File(base, SUBCARPETA_DEFECTO);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        return carpeta;
    }

    private void actualizarTextoRuta() {
        DocumentFile docDir = obtenerCarpetaDocumento();
        if (docDir != null) {
            String nombre = docDir.getName();
            textoRutaActual.setText("Ruta seleccionada: " + (nombre != null ? nombre : docDir.getUri().toString()));
        } else {
            textoRutaActual.setText("Ruta por defecto: " + obtenerCarpetaLocalDefecto().getAbsolutePath());
        }
    }

    private String construirNombreArchivo(String titulo) {
        String limpio = titulo.trim().replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        if (limpio.isEmpty()) {
            limpio = "nota_" + System.currentTimeMillis();
        }
        if (!limpio.toLowerCase().endsWith(".txt")) {
            limpio = limpio + ".txt";
        }
        return limpio;
    }

    private void guardarNota() {
        String titulo = campoTitulo.getText().toString().trim();
        String cuerpo = campoCuerpo.getText().toString();

        if (titulo.isEmpty() && cuerpo.trim().isEmpty()) {
            estadoInicio.setText("Nota vacia");
            return;
        }

        String contenidoArchivo = titulo.isEmpty() ? cuerpo : (titulo + "\n\n" + cuerpo);
        byte[] bytes = contenidoArchivo.getBytes(StandardCharsets.UTF_8);

        try {
            DocumentFile carpetaDoc = obtenerCarpetaDocumento();
            if (carpetaDoc != null && carpetaDoc.canWrite()) {
                if (archivoEditandoDoc != null && archivoEditandoDoc.exists()) {
                    String nombreDeseado = construirNombreArchivo(titulo);
                    if (!nombreDeseado.equalsIgnoreCase(archivoEditandoDoc.getName())) {
                        try {
                            archivoEditandoDoc.renameTo(nombreDeseado);
                        } catch (Exception ignored) {
                        }
                    }
                    OutputStream os = getContentResolver().openOutputStream(archivoEditandoDoc.getUri(), "wt");
                    if (os != null) {
                        os.write(bytes);
                        os.close();
                    }
                } else {
                    String nombreArchivo = construirNombreArchivo(titulo);
                    DocumentFile existente = carpetaDoc.findFile(nombreArchivo);
                    DocumentFile destino = (existente != null && existente.isFile())
                            ? existente
                            : carpetaDoc.createFile("text/plain", nombreArchivo);
                    if (destino == null) {
                        estadoInicio.setText("No se pudo crear el archivo");
                        return;
                    }
                    OutputStream os = getContentResolver().openOutputStream(destino.getUri(), "wt");
                    if (os != null) {
                        os.write(bytes);
                        os.close();
                    }
                }
            } else {
                File carpetaLocal = obtenerCarpetaLocalDefecto();
                File destino;
                if (archivoEditandoFile != null) {
                    String nombreDeseado = construirNombreArchivo(titulo);
                    destino = new File(carpetaLocal, nombreDeseado);
                    if (archivoEditandoFile.exists() && !archivoEditandoFile.getAbsolutePath().equals(destino.getAbsolutePath())) {
                        archivoEditandoFile.delete();
                    }
                } else {
                    destino = new File(carpetaLocal, construirNombreArchivo(titulo));
                }
                FileOutputStream fos = new FileOutputStream(destino, false);
                fos.write(bytes);
                fos.close();
            }

            archivoEditandoDoc = null;
            archivoEditandoFile = null;
            campoTitulo.setText("");
            campoCuerpo.setText("");
            vibrarCorto();
            estadoInicio.setText("Guardada");
        } catch (Exception e) {
            estadoInicio.setText("Error al guardar: " + e.getMessage());
        }
    }

    private void cargarNotasGuardadas() {
        entradasNotas.clear();
        List<String> nombres = new ArrayList<>();

        DocumentFile carpetaDoc = obtenerCarpetaDocumento();
        if (carpetaDoc != null && carpetaDoc.canRead()) {
            DocumentFile[] hijos = carpetaDoc.listFiles();
            List<DocumentFile> txtDocs = new ArrayList<>();
            for (DocumentFile f : hijos) {
                if (f != null && f.isFile() && f.getName() != null && f.getName().toLowerCase().endsWith(".txt")) {
                    txtDocs.add(f);
                }
            }
            txtDocs.sort(Comparator.comparingLong(DocumentFile::lastModified).reversed());
            for (DocumentFile f : txtDocs) {
                entradasNotas.add(new EntradaNota(f.getName(), f, null));
                nombres.add(f.getName());
            }
        } else {
            File carpetaLocal = obtenerCarpetaLocalDefecto();
            File[] archivos = carpetaLocal.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            if (archivos != null) {
                Arrays.sort(archivos, Comparator.comparingLong(File::lastModified).reversed());
                for (File f : archivos) {
                    entradasNotas.add(new EntradaNota(f.getName(), null, f));
                    nombres.add(f.getName());
                }
            }
        }

        if (nombres.isEmpty()) {
            nombres.add("Sin notas guardadas");
            estadoGuardados.setText("");
        } else {
            estadoGuardados.setText("Total: " + entradasNotas.size());
        }

        adaptadorNotas.clear();
        adaptadorNotas.addAll(nombres);
        adaptadorNotas.notifyDataSetChanged();
    }

    private String leerTextoCompleto(EntradaNota entrada) throws Exception {
        InputStream is;
        if (entrada.docFile != null) {
            is = getContentResolver().openInputStream(entrada.docFile.getUri());
        } else if (entrada.localFile != null) {
            is = new FileInputStream(entrada.localFile);
        } else {
            return "";
        }
        if (is == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr, 8192);
        char[] buffer = new char[8192];
        int leidos;
        while ((leidos = br.read(buffer)) != -1) {
            sb.append(buffer, 0, leidos);
        }
        br.close();
        isr.close();
        is.close();
        return sb.toString();
    }

    private void abrirModoLectura(EntradaNota entrada) {
        if (entrada.docFile == null && entrada.localFile == null) {
            return;
        }
        try {
            String contenido = leerTextoCompleto(entrada);
            visorLectura.setText(contenido);
            estadoGuardados.setText("Lectura: " + entrada.nombre);
        } catch (Exception e) {
            visorLectura.setText("");
            estadoGuardados.setText("Error al leer");
        }
    }

    private void mostrarOpcionesNota(EntradaNota entrada) {
        if (entrada.docFile == null && entrada.localFile == null) {
            return;
        }
        String[] opciones = {"Eliminar", "Editar", "Compartir"};
        new AlertDialog.Builder(this)
                .setTitle(entrada.nombre)
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        eliminarNota(entrada);
                    } else if (which == 1) {
                        cargarNotaParaEditar(entrada);
                    } else if (which == 2) {
                        compartirNota(entrada);
                    }
                })
                .show();
    }

    private void eliminarNota(EntradaNota entrada) {
        boolean borrado = false;
        try {
            if (entrada.docFile != null) {
                borrado = entrada.docFile.delete();
            } else if (entrada.localFile != null) {
                borrado = entrada.localFile.delete();
            }
        } catch (Exception ignored) {
        }
        if (borrado) {
            visorLectura.setText("");
            vibrarCorto();
            estadoGuardados.setText("Eliminada: " + entrada.nombre);
            cargarNotasGuardadas();
        } else {
            estadoGuardados.setText("No se pudo eliminar");
        }
    }

    private void cargarNotaParaEditar(EntradaNota entrada) {
        try {
            String contenido = leerTextoCompleto(entrada);
            String nombreSinTxt = entrada.nombre.replaceAll("(?i)\\.txt$", "").replace('_', ' ');
            String tituloInicial = nombreSinTxt;
            String cuerpoInicial = contenido;

            int saltoDoble = contenido.indexOf("\n\n");
            if (saltoDoble > 0) {
                String primeraLinea = contenido.substring(0, saltoDoble).trim();
                if (!primeraLinea.isEmpty()) {
                    tituloInicial = primeraLinea;
                    cuerpoInicial = contenido.substring(saltoDoble + 2);
                }
            }

            archivoEditandoDoc = entrada.docFile;
            archivoEditandoFile = entrada.localFile;
            campoTitulo.setText(tituloInicial);
            campoCuerpo.setText(cuerpoInicial);
            estadoInicio.setText("Editando: " + entrada.nombre);
            mostrarSeccion("inicio");
        } catch (Exception e) {
            estadoGuardados.setText("Error al cargar para editar");
        }
    }

    private void compartirNota(EntradaNota entrada) {
        try {
            String contenido = leerTextoCompleto(entrada);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, entrada.nombre);
            intent.putExtra(Intent.EXTRA_TEXT, contenido);
            startActivity(Intent.createChooser(intent, "Compartir nota"));
        } catch (Exception e) {
            estadoGuardados.setText("Error al compartir");
        }
    }

    private void vibrarCorto() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
