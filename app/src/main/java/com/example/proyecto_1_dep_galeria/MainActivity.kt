package com.example.proyecto_1_dep_galeria

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

data class Foto(val uri: Uri, var esFav: Boolean = false)

class DrawingView @JvmOverloads constructor(c: Context, a: AttributeSet? = null) : View(c, a) {
    data class Trazo(val path: Path, val color: Int, val width: Float = 10f)
    val trazos = mutableListOf<Trazo>()
    private var pathActual = Path()
    private var lastX = 0f; private var lastY = 0f
    var colorActual = 0xFFEF4444.toInt()
    var isDrawingEnabled = false
    var modoRecorte = false
    val cropRect = RectF()
    private var handle = -1
    private var touchX = 0f; private var touchY = 0f

    private val pStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val pFill = Paint(Paint.ANTI_ALIAS_FLAG)

    fun undo() { if (trazos.isNotEmpty()) { trazos.removeLast(); invalidate() } }
    fun clear() { trazos.clear(); pathActual.reset(); invalidate() }

    fun iniciarRecorte(r: RectF) {
        modoRecorte = true
        val w = r.width() * 0.4f; val h = r.height() * 0.4f
        cropRect.set(r.centerX() - w, r.centerY() - h, r.centerX() + w, r.centerY() + h)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (t in trazos) {
            pStroke.color = t.color; pStroke.strokeWidth = t.width
            canvas.drawPath(t.path, pStroke)
        }
        if (!pathActual.isEmpty) {
            pStroke.color = colorActual; pStroke.strokeWidth = 10f
            canvas.drawPath(pathActual, pStroke)
        }
        if (modoRecorte && !cropRect.isEmpty) {
            pFill.color = 0x99000000.toInt(); pFill.style = Paint.Style.FILL
            val w = width.toFloat(); val h = height.toFloat()
            canvas.drawRect(0f, 0f, w, cropRect.top, pFill)
            canvas.drawRect(0f, cropRect.bottom, w, h, pFill)
            canvas.drawRect(0f, cropRect.top, cropRect.left, cropRect.bottom, pFill)
            canvas.drawRect(cropRect.right, cropRect.top, w, cropRect.bottom, pFill)

            pStroke.color = Color.WHITE; pStroke.strokeWidth = 4f
            canvas.drawRect(cropRect, pStroke)

            pFill.color = Color.WHITE
            val pts = floatArrayOf(
                cropRect.left, cropRect.top, cropRect.right, cropRect.top,
                cropRect.left, cropRect.bottom, cropRect.right, cropRect.bottom
            )
            for (i in pts.indices step 2) canvas.drawCircle(pts[i], pts[i + 1], 16f, pFill)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x; val y = e.y
        if (modoRecorte) {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    val pts = floatArrayOf(
                        cropRect.left, cropRect.top, cropRect.right, cropRect.top,
                        cropRect.left, cropRect.bottom, cropRect.right, cropRect.bottom
                    )
                    handle = -1
                    for (i in pts.indices step 2) {
                        val dx = x - pts[i]; val dy = y - pts[i + 1]
                        if (dx * dx + dy * dy < 3600f) { handle = i / 2; break }
                    }
                    if (handle == -1 && cropRect.contains(x, y)) handle = 4
                    touchX = x; touchY = y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = x - touchX; val dy = y - touchY
                    when (handle) {
                        0 -> { cropRect.left = (cropRect.left + dx).coerceAtMost(cropRect.right - 80); cropRect.top = (cropRect.top + dy).coerceAtMost(cropRect.bottom - 80) }
                        1 -> { cropRect.right = (cropRect.right + dx).coerceAtLeast(cropRect.left + 80); cropRect.top = (cropRect.top + dy).coerceAtMost(cropRect.bottom - 80) }
                        2 -> { cropRect.left = (cropRect.left + dx).coerceAtMost(cropRect.right - 80); cropRect.bottom = (cropRect.bottom + dy).coerceAtLeast(cropRect.top + 80) }
                        3 -> { cropRect.right = (cropRect.right + dx).coerceAtLeast(cropRect.left + 80); cropRect.bottom = (cropRect.bottom + dy).coerceAtLeast(cropRect.top + 80) }
                        4 -> cropRect.offset(dx, dy)
                    }
                    touchX = x; touchY = y; invalidate()
                }
                MotionEvent.ACTION_UP -> handle = -1
            }
            return true
        }
        if (!isDrawingEnabled) return false
        when (e.action) {
            MotionEvent.ACTION_DOWN -> { pathActual = Path().apply { moveTo(x, y) }; lastX = x; lastY = y }
            MotionEvent.ACTION_MOVE -> { pathActual.quadTo(lastX, lastY, (x + lastX) / 2f, (y + lastY) / 2f); lastX = x; lastY = y }
            MotionEvent.ACTION_UP -> { pathActual.lineTo(x, y); trazos.add(Trazo(pathActual, colorActual)); pathActual = Path() }
        }
        invalidate(); return true
    }

    fun dibujarTrazos(canvas: Canvas, scale: Float) {
        for (t in trazos) {
            pStroke.color = t.color; pStroke.strokeWidth = t.width * scale
            canvas.drawPath(t.path, pStroke)
        }
    }
}

class MainActivity : AppCompatActivity() {
    private val fotos = mutableListOf<Foto>()
    private var indice = -1
    private var soloFav = false
    private var bmpEdit: Bitmap? = null

    private val iv by lazy { findViewById<ImageView>(R.id.imageView) }
    private val dv by lazy { findViewById<DrawingView>(R.id.drawingView) }
    private val ivFav by lazy { findViewById<ImageView>(R.id.ivFavOverlay) }
    private val btnFav by lazy { findViewById<MaterialButton>(R.id.btnFavorito) }
    private val btnAnt by lazy { findViewById<View>(R.id.btnAnterior) }
    private val btnSig by lazy { findViewById<View>(R.id.btnSiguiente) }
    private val pGaleria by lazy { findViewById<View>(R.id.panelGaleria) }
    private val pEdicion by lazy { findViewById<View>(R.id.panelEdicion) }
    private val tvTitulo by lazy { findViewById<TextView>(R.id.tvTituloEdicion) }
    private val rowColores by lazy { findViewById<View>(R.id.rowColores) }

    private val listaActual get() = if (soloFav) fotos.filter { it.esFav } else fotos
    private val fotoActual get() = fotos.getOrNull(indice)

      private val pickMedia = registerForActivityResult(PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) {
            fotos.addAll(uris.map { Foto(it) })
            if (indice == -1) indice = 0
            actualizarVista()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        registerForActivityResult(RequestMultiplePermissions()) {}.launch(arrayOf(perm))

        btnAnt.setOnClickListener { navegar(-1) }
        btnSig.setOnClickListener { navegar(1) }
        btnFav.setOnClickListener {
            fotoActual?.let {
                it.esFav = !it.esFav
                if (soloFav && !it.esFav) sincronizarIndice()
                actualizarVista()
            }
        }
        findViewById<View>(R.id.btnEditar).setOnClickListener { entrarEdicion() }
        findViewById<View>(R.id.btnEliminar).setOnClickListener {
            if (indice in fotos.indices) {
                fotos.removeAt(indice)
                sincronizarIndice()
                actualizarVista()
            }
        }
        findViewById<View>(R.id.btnCancelarEdicion).setOnClickListener { salirEdicion() }
        findViewById<View>(R.id.btnGuardarEdicion).setOnClickListener { guardarEdicion() }
        findViewById<View>(R.id.btnHerramientaDibujar).setOnClickListener {
            dv.modoRecorte = false; dv.isDrawingEnabled = true; rowColores.visibility = View.VISIBLE
            tvTitulo.text = "✏ Pincel"; dv.invalidate()
        }
        findViewById<View>(R.id.btnHerramientaRecortar).setOnClickListener {
            if (!dv.modoRecorte) {
                val b = bmpEdit; val w = iv.width.toFloat(); val h = iv.height.toFloat()
                val s = if (b != null) minOf(w / b.width, h / b.height) else 1f
                val dx = if (b != null) (w - b.width * s) / 2f else 0f
                val dy = if (b != null) (h - b.height * s) / 2f else 0f
                dv.iniciarRecorte(if (b != null) RectF(dx, dy, dx + b.width * s, dy + b.height * s) else RectF(0f, 0f, w, h))
                dv.isDrawingEnabled = false; rowColores.visibility = View.GONE
                tvTitulo.text = "✂ Toca otra vez para aplicar"
            } else aplicarRecorte()
        }
        findViewById<View>(R.id.btnHerramientaRotar).setOnClickListener {
            val b = bmpEdit ?: return@setOnClickListener
            val nuevo = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(90f) }, true)
            if (nuevo != b) b.recycle()
            bmpEdit = nuevo; iv.setImageBitmap(nuevo); dv.clear()
        }
        findViewById<View>(R.id.btnHerramientaDeshacer).setOnClickListener { dv.undo() }

        val colores = listOf(
            R.id.colorRojo to 0xFFEF4444.toInt(), R.id.colorAzul to 0xFF3B82F6.toInt(),
            R.id.colorVerde to 0xFF10B981.toInt(), R.id.colorAmarillo to 0xFFF59E0B.toInt(),
            R.id.colorNegro to 0xFF1F2937.toInt(), R.id.colorBlanco to 0xFFFFFFFF.toInt()
        )
        colores.forEach { (id, col) ->
            findViewById<View>(id).setOnClickListener {
                dv.colorActual = col; dv.isDrawingEnabled = true; dv.modoRecorte = false; dv.invalidate()
            }
        }
    }

    private fun sincronizarIndice() {
        val l = listaActual
        indice = if (l.isNotEmpty()) fotos.indexOf(l[0]) else -1
    }

    private fun navegar(delta: Int) {
        val l = listaActual; if (l.isEmpty()) return
        val pos = (l.indexOf(fotoActual).coerceAtLeast(0) + delta).coerceIn(0, l.size - 1)
        indice = fotos.indexOf(l[pos])
        actualizarVista()
    }

    private fun actualizarVista() {
        val f = fotoActual
        iv.setImageURI(f?.uri)
        ivFav.visibility = if (f?.esFav == true) View.VISIBLE else View.GONE
        btnFav.setIconResource(if (f?.esFav == true) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
        btnFav.text = if (f?.esFav == true) "❤ Fav" else "♡ Fav"
        val count = if (soloFav) fotos.count { it.esFav } else fotos.size
        supportActionBar?.title = "${if (soloFav) "Favoritos" else "Galería"} ($count)"
        invalidateOptionsMenu()
    }

    private fun entrarEdicion() {
        val f = fotoActual ?: return Toast.makeText(this, "Selecciona una imagen", Toast.LENGTH_SHORT).show()
        bmpEdit = try {
            contentResolver.openInputStream(f.uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inMutable = true })
            }
        } catch (e: Exception) { null } ?: return Toast.makeText(this, "Error al cargar imagen", Toast.LENGTH_SHORT).show()

        iv.setImageBitmap(bmpEdit)
        dv.clear(); dv.modoRecorte = false; dv.isDrawingEnabled = true
        toggleModoEdicion(true)
    }

    private fun salirEdicion() {
        iv.setImageDrawable(null)
        bmpEdit?.recycle(); bmpEdit = null
        dv.clear(); dv.modoRecorte = false; dv.isDrawingEnabled = false
        toggleModoEdicion(false)
        actualizarVista()
    }

    private fun toggleModoEdicion(enEdicion: Boolean) {
        val vEdit = if (enEdicion) View.VISIBLE else View.GONE
        val vGal = if (enEdicion) View.GONE else View.VISIBLE
        pGaleria.visibility = vGal; btnAnt.visibility = vGal; btnSig.visibility = vGal; ivFav.visibility = View.GONE
        pEdicion.visibility = vEdit; rowColores.visibility = vEdit
        tvTitulo.text = "✏ Pincel"
    }

    private fun aplicarRecorte() {
        val b = bmpEdit ?: return
        val w = iv.width.toFloat(); val h = iv.height.toFloat()
        if (w <= 0 || h <= 0) return
        val s = minOf(w / b.width, h / b.height)
        val dx = (w - b.width * s) / 2f; val dy = (h - b.height * s) / 2f
        val r = dv.cropRect
        val x = ((r.left - dx) / s).toInt().coerceIn(0, b.width - 1)
        val y = ((r.top - dy) / s).toInt().coerceIn(0, b.height - 1)
        val bw = (((r.right - dx) / s) - x).toInt().coerceIn(1, b.width - x)
        val bh = (((r.bottom - dy) / s) - y).toInt().coerceIn(1, b.height - y)

        val nuevo = Bitmap.createBitmap(b, x, y, bw, bh)
        if (nuevo != b) b.recycle()
        bmpEdit = nuevo; iv.setImageBitmap(nuevo)
        dv.clear(); dv.modoRecorte = false; dv.isDrawingEnabled = true
        rowColores.visibility = View.VISIBLE; tvTitulo.text = "Recorte aplicado"
    }

    private fun guardarEdicion() {
        val b = bmpEdit ?: return
        val w = iv.width.toFloat(); val h = iv.height.toFloat()
        val s = minOf(w / b.width, h / b.height)
        val dx = (w - b.width * s) / 2f; val dy = (h - b.height * s) / 2f

        val res = b.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(res).apply {
            concat(Matrix().apply { postTranslate(-dx, -dy); postScale(1f / s, 1f / s) })
        }
        dv.dibujarTrazos(canvas, 1f / s)

        val v = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "Edit_${System.currentTimeMillis()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MiGaleria")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { res.compress(Bitmap.CompressFormat.JPEG, 95, it) }
            fotos.add(Foto(uri))
            indice = fotos.size - 1
            Toast.makeText(this, "¡Guardado!", Toast.LENGTH_SHORT).show()
        }
        res.recycle()
        salirEdicion()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Filtro").apply {
            setIcon(if (soloFav) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.add(0, 2, 1, "Añadir").apply {
            setIcon(android.R.drawable.ic_input_add)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> { soloFav = !soloFav; sincronizarIndice(); actualizarVista() }
            2 -> pickMedia.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        bmpEdit?.recycle()
    }
}