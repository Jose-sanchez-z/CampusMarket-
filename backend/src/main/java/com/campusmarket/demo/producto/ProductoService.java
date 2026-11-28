package com.campusmarket.demo.producto;

import com.campusmarket.demo.catalogo.CatalogoException.*;
import com.campusmarket.demo.categoria.Categoria;
import com.campusmarket.demo.categoria.CategoriaRepository;
import com.campusmarket.demo.categoria.CategoriaService;
import com.campusmarket.demo.producto.ImagenStorageService.ArchivoGuardado;
import com.campusmarket.demo.sesion.Usuario;
import com.campusmarket.demo.sesion.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Lógica de negocio para productos, catálogo simple, edición e imágenes (S2-T11 / S2-T12). */
@Service
public class ProductoService {

    public static final int MAX_IMAGENES_POR_PRODUCTO = 5;
    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            Producto.ESTADO_DISPONIBLE,
            Producto.ESTADO_INACTIVO,
            Producto.ESTADO_VENDIDO
    );

    public record ImagenDto(Long id, String url, String tipoMime, Long tamanoBytes) {
        static ImagenDto desde(ImagenProducto img) {
            return new ImagenDto(img.getId(), "/imagenes/" + img.getNombreArchivo(), img.getTipoMime(), img.getTamanoBytes());
        }
    }

    public record ProductoResponse(
            Long id,
            String titulo,
            String descripcion,
            BigDecimal precio,
            BigDecimal precioFinal,
            String estado,
            Long categoriaId,
            String categoriaNombre,
            Long vendedorId,
            String vendedorNombre,
            String imagenPrincipal,
            List<ImagenDto> imagenes,
            Instant fechaCreacion
    ) {}

    private final ProductoRepository productos;
    private final ImagenProductoRepository imagenes;
    private final CategoriaRepository categorias;
    private final CategoriaService categoriaService;
    private final UsuarioRepository usuarios;
    private final ImagenStorageService storage;

    public ProductoService(ProductoRepository productos,
                           ImagenProductoRepository imagenes,
                           CategoriaRepository categorias,
                           CategoriaService categoriaService,
                           UsuarioRepository usuarios,
                           ImagenStorageService storage) {
        this.productos = productos;
        this.imagenes = imagenes;
        this.categorias = categorias;
        this.categoriaService = categoriaService;
        this.usuarios = usuarios;
        this.storage = storage;
    }

    /**
     * Función reutilizable de titularidad ("¿este producto es de este usuario?").
     */
    @Transactional(readOnly = true)
    public boolean esPropietario(Long productoId, Long usuarioId) {
        if (productoId == null || usuarioId == null) {
            return false;
        }
        return productos.findById(productoId)
                .map(p -> p.getVendedorId().equals(usuarioId))
                .orElse(false);
    }

    /** Verifica que el producto exista y pertenezca al usuario; si no, lanza 404 o 403. */
    @Transactional(readOnly = true)
    public Producto verificarPropietario(Long productoId, Usuario usuario) {
        Producto producto = productos.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontrado("Producto no encontrado"));
        return exigirPropietario(producto, usuario);
    }

    private Producto exigirPropietario(Producto producto, Usuario usuario) {
        if (usuario == null || !producto.getVendedorId().equals(usuario.getId())) {
            throw new AccesoDenegado("No tienes permiso para modificar este producto");
        }
        return producto;
    }

    @Transactional
    public ProductoResponse crear(Usuario vendedor, String titulo, String descripcion, Long categoriaId, BigDecimal precio) {
        if (vendedor == null || !"ACTIVA".equals(vendedor.getEstado())) {
            throw new AccesoDenegado("Solo vendedores activos pueden publicar productos");
        }
        String tituloLimpio = validarTexto(titulo, "El título es obligatorio", 150, "El título no puede superar 150 caracteres");
        String descLimpia = validarTexto(descripcion, "La descripción es obligatoria", 10000, "La descripción es demasiado larga");
        BigDecimal precioValido = validarPrecio(precio);
        if (categoriaId == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }
        Categoria categoria = categoriaService.obtenerActiva(categoriaId);

        Producto guardado = productos.save(new Producto(
                vendedor.getId(),
                categoria.getId(),
                tituloLimpio,
                descLimpia,
                precioValido
        ));

        return armarRespuesta(guardado, categoria.getNombre(), vendedor.getNombre(), List.of());
    }

    @Transactional
    public ProductoResponse crearConImagen(Usuario vendedor, String titulo, String descripcion,
                                           Long categoriaId, BigDecimal precio, MultipartFile archivo) {
        ProductoResponse creado = crear(vendedor, titulo, descripcion, categoriaId, precio);
        guardarImagen(creado.id(), archivo);
        return obtenerPorId(creado.id(), vendedor);
    }

    @Transactional
    public ProductoResponse editar(Long productoId, Usuario vendedor, String titulo, String descripcion, Long categoriaId, BigDecimal precio) {
        Producto producto = verificarPropietario(productoId, vendedor);

        if (Producto.ESTADO_VENDIDO.equals(producto.getEstado())) {
            throw new ReglaNegocio("No se puede editar un producto marcado como vendido");
        }

        String tituloLimpio = validarTexto(titulo, "El título es obligatorio", 150, "El título no puede superar 150 caracteres");
        String descLimpia = validarTexto(descripcion, "La descripción es obligatoria", 10000, "La descripción es demasiado larga");
        BigDecimal precioValido = validarPrecio(precio);
        if (categoriaId == null) {
            throw new IllegalArgumentException("La categoría es obligatoria");
        }

        if (!categoriaId.equals(producto.getCategoriaId())) {
            categoriaService.obtenerActiva(categoriaId);
            producto.setCategoriaId(categoriaId);
        }

        producto.setTitulo(tituloLimpio);
        producto.setDescripcion(descLimpia);
        producto.setPrecio(precioValido);

        Producto actualizado = productos.save(producto);
        return enriquecerLista(List.of(actualizado)).get(0);
    }

    @Transactional
    public ProductoResponse cambiarEstado(Long productoId, Usuario vendedor, String nuevoEstadoRaw) {
        Producto producto = verificarPropietario(productoId, vendedor);

        if (nuevoEstadoRaw == null || nuevoEstadoRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        String nuevoEstado = nuevoEstadoRaw.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(nuevoEstado)) {
            throw new IllegalArgumentException("Estado inválido. Valores permitidos: DISPONIBLE, INACTIVO, VENDIDO");
        }
        if (Producto.ESTADO_VENDIDO.equals(producto.getEstado()) && !Producto.ESTADO_VENDIDO.equals(nuevoEstado)) {
            throw new ReglaNegocio("Un producto vendido no puede volver a activarse");
        }

        producto.setEstado(nuevoEstado);
        Producto actualizado = productos.save(producto);
        return enriquecerLista(List.of(actualizado)).get(0);
    }

    @Transactional
    public ImagenDto agregarImagen(Long productoId, Usuario vendedor, MultipartFile archivo) {
        // El bloqueo del producto mantiene el recuento y la inserción en la misma sección crítica.
        Producto producto = productos.buscarParaActualizar(productoId)
                .orElseThrow(() -> new RecursoNoEncontrado("Producto no encontrado"));
        exigirPropietario(producto, vendedor);

        List<ImagenProducto> actuales = imagenes.findByProductoIdOrderByIdAsc(productoId);
        if (actuales.size() >= MAX_IMAGENES_POR_PRODUCTO) {
            throw new ReglaNegocio("Un producto no puede tener más de " + MAX_IMAGENES_POR_PRODUCTO + " imágenes");
        }

        return guardarImagen(productoId, archivo);
    }

    private ImagenDto guardarImagen(Long productoId, MultipartFile archivo) {
        ArchivoGuardado guardada = storage.guardar(archivo);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    storage.eliminarSiExiste(guardada.nombreArchivo());
                }
            }
        });
        ImagenProducto entidad = imagenes.save(new ImagenProducto(
                productoId, guardada.nombreArchivo(), guardada.tipoMime(), guardada.tamanoBytes()));
        return ImagenDto.desde(entidad);
    }

    @Transactional
    public void eliminarImagen(Long productoId, Long imagenId, Usuario vendedor) {
        verificarPropietario(productoId, vendedor);

        ImagenProducto imagen = imagenes.findById(imagenId)
                .orElseThrow(() -> new RecursoNoEncontrado("Imagen no encontrada"));

        if (!imagen.getProductoId().equals(productoId)) {
            throw new RecursoNoEncontrado("La imagen no pertenece a este producto");
        }

        imagenes.delete(imagen);
        storage.eliminarSiExiste(imagen.getNombreArchivo());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarDisponibles() {
        List<Producto> lista = productos.findByEstadoOrderByFechaCreacionDescIdDesc(Producto.ESTADO_DISPONIBLE);
        return enriquecerLista(lista);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listarMios(Usuario vendedor) {
        if (vendedor == null) {
            throw new AccesoDenegado("No autenticado");
        }
        return enriquecerLista(productos.findByVendedorIdOrderByFechaCreacionDescIdDesc(vendedor.getId()));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id, Usuario usuarioActual) {
        Producto p = productos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontrado("Producto no encontrado"));

        boolean esDuenoOAdmin = usuarioActual != null
                && (p.getVendedorId().equals(usuarioActual.getId()) || "ADMIN".equals(usuarioActual.getRol()));
        if (Producto.ESTADO_INACTIVO.equals(p.getEstado()) && !esDuenoOAdmin) {
            throw new RecursoNoEncontrado("Producto no encontrado");
        }

        return enriquecerLista(List.of(p)).get(0);
    }

    /** Carga en lote categorías, vendedores e imágenes para evitar consultas N+1. */
    public List<ProductoResponse> enriquecerLista(List<Producto> lista) {
        if (lista.isEmpty()) {
            return List.of();
        }
        Set<Long> catIds = lista.stream().map(Producto::getCategoriaId).collect(Collectors.toSet());
        Set<Long> vendIds = lista.stream().map(Producto::getVendedorId).collect(Collectors.toSet());
        List<Long> prodIds = lista.stream().map(Producto::getId).toList();

        Map<Long, String> nombresCat = categorias.findAllById(catIds).stream()
                .collect(Collectors.toMap(Categoria::getId, Categoria::getNombre));
        Map<Long, String> nombresVend = usuarios.findAllById(vendIds).stream()
                .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
        Map<Long, List<ImagenDto>> imgsPorProd = imagenes.findByProductoIdInOrderByIdAsc(prodIds).stream()
                .collect(Collectors.groupingBy(
                        ImagenProducto::getProductoId,
                        Collectors.mapping(ImagenDto::desde, Collectors.toList())
                ));

        return lista.stream()
                .map(p -> armarRespuesta(
                        p,
                        nombresCat.getOrDefault(p.getCategoriaId(), ""),
                        nombresVend.getOrDefault(p.getVendedorId(), ""),
                        imgsPorProd.getOrDefault(p.getId(), List.of())
                ))
                .toList();
    }

    private ProductoResponse armarRespuesta(Producto p, String catNombre, String vendNombre, List<ImagenDto> imgs) {
        String principal = imgs.isEmpty() ? null : imgs.get(0).url();
        return new ProductoResponse(
                p.getId(),
                p.getTitulo(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getPrecio(),
                p.getEstado(),
                p.getCategoriaId(),
                catNombre,
                p.getVendedorId(),
                vendNombre,
                principal,
                imgs,
                p.getFechaCreacion()
        );
    }

    public static String validarTexto(String valor, String msgVacio, int maxLen, String msgLargo) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException(msgVacio);
        }
        String limpio = valor.trim();
        if (limpio.length() > maxLen) {
            throw new IllegalArgumentException(msgLargo);
        }
        return limpio;
    }

    public static BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null) {
            throw new IllegalArgumentException("El precio es obligatorio");
        }
        if (precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ReglaNegocio("El precio debe ser mayor que cero");
        }
        return precio.setScale(2, RoundingMode.HALF_UP);
    }
}
