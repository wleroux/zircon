package org.hexworks.zircon.internal.component.renderer

import org.hexworks.zircon.api.component.renderer.ComponentRenderContext
import org.hexworks.zircon.api.component.renderer.ComponentRenderer
import org.hexworks.zircon.api.graphics.impl.SubTileGraphics
import org.hexworks.zircon.internal.component.impl.DefaultButton

class DefaultButtonRenderer : ComponentRenderer<DefaultButton> {

    override fun render(tileGraphics: SubTileGraphics, context: ComponentRenderContext<DefaultButton>) {
        val style = context.componentStyle.currentStyle()
        tileGraphics.applyStyle(style)
        tileGraphics.putText(context.component.text)
    }

}
