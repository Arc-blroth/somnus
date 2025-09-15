package ai.arcblroth.somnus3.panel

import dev.kord.common.entity.ButtonStyle
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.modify.MessageModifyBuilder

class InteractivePanel(
    val style: ButtonStyle,
    val pages: List<InteractivePanelPage>,
) {
    private var lastBuiltPage = -1
    private var page = 0

    fun nextPage() {
        if (page < pages.size - 1) {
            page += 1
        }
    }

    fun previousPage() {
        if (page > 0) {
            page -= 1
        }
    }

    fun updatePage(builder: MessageModifyBuilder) {
        if (page != lastBuiltPage) {
            lastBuiltPage = page
            builder.embeds = mutableListOf(EmbedBuilder().apply(pages[page]))
        }
    }
}

typealias InteractivePanelPage = EmbedBuilder.() -> Unit

interface InteractivePanelBuilder {
    fun style(style: ButtonStyle)

    fun page(page: InteractivePanelPage)
}

class InteractivePanelBuilderImpl : InteractivePanelBuilder {
    var style: ButtonStyle = ButtonStyle.Primary
    private val pages: MutableList<InteractivePanelPage> = mutableListOf()

    override fun style(style: ButtonStyle) {
        this.style = style
    }

    override fun page(page: InteractivePanelPage) {
        pages.add(page)
    }

    fun build() = InteractivePanel(style, pages)
}
