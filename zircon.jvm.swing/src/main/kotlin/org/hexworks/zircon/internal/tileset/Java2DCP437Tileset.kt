package org.hexworks.zircon.internal.tileset

import com.github.benmanes.caffeine.cache.Caffeine
import org.hexworks.cobalt.datatypes.Identifier
import org.hexworks.cobalt.datatypes.factory.IdentifierFactory
import org.hexworks.zircon.api.data.CharacterTile
import org.hexworks.zircon.api.data.Position
import org.hexworks.zircon.api.data.Tile
import org.hexworks.zircon.api.modifier.*
import org.hexworks.zircon.api.resource.TileType.CHARACTER_TILE
import org.hexworks.zircon.api.resource.TilesetResource
import org.hexworks.zircon.api.tileset.TextureTransformer
import org.hexworks.zircon.api.tileset.TileTexture
import org.hexworks.zircon.api.tileset.Tileset
import org.hexworks.zircon.api.tileset.impl.CP437TileMetadataLoader
import org.hexworks.zircon.internal.tileset.impl.DefaultTileTexture
import org.hexworks.zircon.internal.tileset.transformer.*
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
class Java2DCP437Tileset(private val resource: TilesetResource,
                         private val source: BufferedImage)
    : Tileset<Graphics2D> {

    override val id: Identifier = IdentifierFactory.randomIdentifier()
    override val width: Int
        get() = resource.width
    override val height: Int
        get() = resource.height

    private val lookup = CP437TileMetadataLoader(
            width = resource.width,
            height = resource.height)

    private val cache = Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(5000)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build<String, TileTexture<BufferedImage>>()

    override val targetType = Graphics2D::class

    init {
        require(resource.tileType == CHARACTER_TILE) {
            "Can't use a ${resource.tileType.name}-based TilesetResource for" +
                    " a CharacterTile-based tileset."
        }
    }

    override fun drawTile(tile: Tile, surface: Graphics2D, position: Position) {
        val texture = fetchTextureForTile(tile)
        val x = position.x * width
        val y = position.y * height
        surface.drawImage(texture.texture, x, y, null)
    }

    private fun fetchTextureForTile(tile: Tile): TileTexture<BufferedImage> {
        var fixedTile = tile as? CharacterTile ?: throw IllegalArgumentException("Wrong tile type")
        fixedTile.modifiers.filterIsInstance<TileTransformModifier<CharacterTile>>().forEach { modifier ->
            if(modifier.canTransform(fixedTile)) {
                fixedTile = modifier.transform(fixedTile)
            }
        }
        val key = fixedTile.generateCacheKey()
        val meta = lookup.fetchMetaForTile(fixedTile)
        val maybeRegion = cache.getIfPresent(key)
        return if (maybeRegion != null) {
            maybeRegion
        } else {
            var image: TileTexture<BufferedImage> = DefaultTileTexture(
                    width = width,
                    height = height,
                    texture = source.getSubimage(meta.x * width, meta.y * height, width, height))
            TILE_INITIALIZERS.forEach {
                image = it.transform(image, fixedTile)
            }
            fixedTile.modifiers.filterIsInstance<TextureTransformModifier>().forEach {
                image = TEXTURE_TRANSFORMER_LOOKUP[it::class]?.transform(image, fixedTile) ?: image
            }
            cache.put(key, image)
            image
        }
    }

    companion object {

        private val TILE_INITIALIZERS = listOf(
                Java2DTextureCloner(),
                Java2DTextureColorizer())

        val TEXTURE_TRANSFORMER_LOOKUP: Map<KClass<out TextureTransformModifier>, TextureTransformer<BufferedImage>> = mapOf(
                Pair(SimpleModifiers.Underline::class, Java2DUnderlineTransformer()),
                Pair(SimpleModifiers.VerticalFlip::class, Java2DVerticalFlipper()),
                Pair(SimpleModifiers.Blink::class, Java2DNoOpTransformer()),
                Pair(SimpleModifiers.HorizontalFlip::class, Java2DHorizontalFlipper()),
                Pair(SimpleModifiers.CrossedOut::class, Java2DCrossedOutTransformer()),
                Pair(SimpleModifiers.Hidden::class, Java2DHiddenTransformer()),
                Pair(Glow::class, Java2DGlowTransformer()),
                Pair(Border::class, Java2DBorderTransformer()),
                Pair(Crop::class, Java2DCropTransformer()),
                Pair(RayShade::class, Java2DRayShaderTransformer())
        ).toMap()

    }
}
