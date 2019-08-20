package net.teamfruit.emojicord.emoji;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.math.NumberUtils;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Maps;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.teamfruit.emojicord.CoreInvoke;
import net.teamfruit.emojicord.EmojicordConfig;
import net.teamfruit.emojicord.compat.Compat;
import net.teamfruit.emojicord.compat.OpenGL;
import net.teamfruit.emojicord.emoji.EmojiObject.EmojiObjectCache;
import net.teamfruit.emojicord.emoji.EmojiText.EmojiTextCache;
import net.teamfruit.emojicord.emoji.EmojiText.EmojiTextElement;

public class EmojiFontRenderer extends FontRenderer {
	public static final @Nonnull String fontText = "\u00c0\u00c1\u00c2\u00c8\u00ca\u00cb\u00cd\u00d3\u00d4\u00d5\u00da\u00df\u00e3\u00f5\u011f\u0130\u0131\u0152\u0153\u015e\u015f\u0174\u0175\u017e\u0207\u0000\u0000\u0000\u0000\u0000\u0000\u0000 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u0000\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00f8\u00a3\u00d8\u00d7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u00ae\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u03b2\u0393\u03c0\u03a3\u03c3\u03bc\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u2205\u2208\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0\u0000";

	@CoreInvoke
	public static boolean IsNewChatRendering;

	private static final Map<Integer, EmojiObject> CurrentEmojis = Maps.newHashMap();
	private static String CurrentText = "";

	public EmojiFontRenderer(final Minecraft minecraft) {
		super(minecraft.gameSettings, new net.minecraft.util.ResourceLocation("textures/font/ascii.png"),
				minecraft.renderEngine, false);
		super.onResourceManagerReload(minecraft.getResourceManager());
	}

	@Override
	public int getStringWidth(final String text) {
		CurrentText = getEmojiFormattedString(text);
		return super.getStringWidth(CurrentText);
	}

	public static String getEmojiFormattedString(String text) {
		if (EmojicordConfig.renderEnabled&&!StringUtil.isNullOrEmpty(text)) {
			Compat.CompatMinecraft.getMinecraft().mcProfiler.startSection("emojicordParse");
			if (StringUtil.isNullOrEmpty(text))
				return text;
			final EmojiText emojiPair = EmojiTextCache.instance.getEmojiText(text);
			text = emojiPair.text;
			final Matcher matcher = EmojiText.placeHolderPattern.matcher(text);
			final StringBuffer sb = new StringBuffer();
			final List<EmojiTextElement> emojis = emojiPair.emojis;
			while (matcher.find()) {
				final int emojiIndex = NumberUtils.toInt(matcher.group(1), -1);
				if (0<=emojiIndex&&emojiIndex<emojis.size()) {
					final EmojiTextElement entry = emojis.get(emojiIndex);
					final EmojiId emojiId = entry.id;
					final EmojiObject emoji = emojiId==null ? null : EmojiObjectCache.instance.getEmojiObject(emojiId);
					if (emoji==null)
						matcher.appendReplacement(sb, entry.raw);
					else {
						matcher.appendReplacement(sb, "?");
						final int index = sb.length()-"?".length();
						CurrentEmojis.put(index, emoji);
						if (IsNewChatRendering)
							sb.append(entry.raw);
					}
				}
			}
			matcher.appendTail(sb);
			text = sb.toString();
			Compat.CompatMinecraft.getMinecraft().mcProfiler.endSection();
		}
		return text;
	}

	@Override
	public int getCharWidth(final char character) {
		if (character=='?')
			return 10;
		return super.getCharWidth(character);
	}

	@Override
	protected void renderStringAtPos(String text, final boolean hasShadow) {
		if (text.isEmpty())
			return;
		CurrentEmojis.clear();
		text = CurrentText = getEmojiFormattedString(text);
		for (int charIndex = 0; charIndex<text.length(); charIndex++) {
			char character = text.charAt(charIndex);
			if (character=='§'&&charIndex+1<text.length()) {
				int formatting = "0123456789abcdefklmnor"
						.indexOf(text.toLowerCase(Locale.ENGLISH).charAt(charIndex+1));
				if (formatting<16) {
					this.randomStyle = false;
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;
					if (formatting<0||formatting>15)
						formatting = 15;

					if (hasShadow)
						formatting += 16;

					final int colour = this.colorCode[formatting];
					this.textColor = colour;
					setColor((colour>>16)/255.0F, (colour>>8&0xFF)/255.0F, (colour&0xFF)/255.0F,
							this.alpha);
				} else if (formatting==16)
					this.randomStyle = true;

				else if (formatting==17)
					this.boldStyle = true;

				else if (formatting==18)
					this.strikethroughStyle = true;

				else if (formatting==19)
					this.underlineStyle = true;

				else if (formatting==20)
					this.italicStyle = true;

				else if (formatting==21) {
					this.randomStyle = false;
					this.boldStyle = false;
					this.strikethroughStyle = false;
					this.underlineStyle = false;
					this.italicStyle = false;
					setColor(this.red, this.blue, this.green, this.alpha);
				}
				charIndex++;
			} else {
				int c = fontText.indexOf(character);
				if (this.randomStyle&&c!=-1) {
					final int width = getCharWidth(character);
					char newChar;
					for (;;) {
						c = this.fontRandom.nextInt(fontText.length());
						newChar = fontText.charAt(c);
						if (width==getCharWidth(newChar))
							break;
					}
					character = newChar;
				}
				final float size = c==-1||this.unicodeFlag ? 0.5F : 1.0F;
				final boolean shadow = (character==0||c==-1||this.unicodeFlag)&&hasShadow;
				if (shadow) {
					this.posX -= size;
					this.posY -= size;
				}
				float offset = renderChar(character, this.italicStyle, charIndex, hasShadow);
				if (shadow) {
					this.posX += size;
					this.posY += size;
				}
				if (this.boldStyle) {
					this.posX += size;
					if (shadow) {
						this.posX -= size;
						this.posY -= size;
					}
					renderChar(character, this.italicStyle, charIndex, true);
					this.posX -= size;
					if (shadow) {
						this.posX += size;
						this.posY += size;
					}
					offset += 1.0F;
				}
				doDraw(offset);
			}
		}
	}

	private float renderChar(final char c, final boolean italic, final int index, final boolean hasShadow) {
		if (EmojicordConfig.renderEnabled) {
			final EmojiObject emoji = CurrentEmojis.get(index);
			if (emoji!=null)
				if (hasShadow)
					return 10.0F;
				else {
					Compat.CompatMinecraft.getMinecraft().renderEngine.bindTexture(emoji.getResourceLocationForBinding());
					renderEmoji(emoji);
					return 10.0F;
				}
		}
		if (c==' ')
			return 4.0F;

		final int charIndex = fontText.indexOf(c);
		return charIndex!=-1&&!this.unicodeFlag ? renderDefaultChar(charIndex, italic)
				: renderUnicodeChar(c, italic);
	}

	public void renderEmoji(final EmojiObject emoji) {
		final float textureSize = 16.0F;
		final float textureX = 0.0F/textureSize;
		final float textureY = 0.0F/textureSize;
		final float textureOffset = 16.0F/textureSize;
		final float size = 10.0F;
		final float offsetY = 1.0F;
		final float offsetX = 0.0F;

		OpenGL.glPushAttrib();

		//OpenGL.glEnable(GL11.GL_BLEND);
		//OpenGL.glEnable(GL11.GL_ALPHA_TEST);

		OpenGL.glColor4f(1.0F, 1.0F, 1.0F, (OpenGL.glGetColorRGBA()>>24&0xff)/256f);
		OpenGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		//OpenGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
		//OpenGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
		//OpenGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		//OpenGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		OpenGL.glBegin(GL11.GL_QUADS);
		OpenGL.glTexCoord2f(textureX, textureY);
		OpenGL.glVertex3f(this.posX-offsetX, this.posY-offsetY, 0.0F);
		OpenGL.glTexCoord2f(textureX, textureY+textureOffset);
		OpenGL.glVertex3f(this.posX-offsetX, this.posY+size-offsetY, 0.0F);
		OpenGL.glTexCoord2f(textureX+textureOffset, textureY+textureOffset);
		OpenGL.glVertex3f(this.posX-offsetX+size, this.posY+size-offsetY, 0.0F);
		OpenGL.glTexCoord2f(textureX+textureOffset, textureY/textureSize);
		OpenGL.glVertex3f(this.posX-offsetX+size, this.posY-offsetY, 0.0F);
		OpenGL.glEnd();
		//OpenGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
		//OpenGL.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

		//OpenGL.glDisable(GL11.GL_ALPHA_TEST);
		//OpenGL.glDisable(GL11.GL_BLEND);

		OpenGL.glColor4f(this.red, this.green, this.blue, this.alpha);

		OpenGL.glPopAttrib();
	}
}
