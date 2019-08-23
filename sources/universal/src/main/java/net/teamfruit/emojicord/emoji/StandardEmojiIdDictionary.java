package net.teamfruit.emojicord.emoji;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class StandardEmojiIdDictionary {
	public final ImmutableMap<String, EmojiId> aliasDictionary;
	public final ImmutableMap<String, EmojiId> utfDictionary;
	public final Pattern shortAliasPattern;
	public final Pattern utfPattern;

	public StandardEmojiIdDictionary(final ImmutableMap<String, EmojiId> aliasDictionary, final ImmutableMap<String, EmojiId> utfDictionary, final Pattern shortAliasPattern, final Pattern utfPattern) {
		this.aliasDictionary = aliasDictionary;
		this.utfDictionary = utfDictionary;
		this.shortAliasPattern = shortAliasPattern;
		this.utfPattern = utfPattern;
	}

	public static class StandardEmojiIdDictionaryBuilder {
		private final Map<String, EmojiId> aliasDictionary = Maps.newHashMap();
		private final Map<String, EmojiId> utfDictionary = Maps.newHashMap();
		private static final Pattern shortAliasFilterNot = Pattern.compile(".+\\:skin-tone-\\d");
		private static final Pattern shortAliasFilter = Pattern.compile("^.*[^\\w].*$");
		private final Supplier<Set<String>> shortAlias = Suppliers.memoize(() -> this.aliasDictionary.keySet().stream()
				.filter(str -> {
					return !shortAliasFilterNot.matcher(str).matches()&&shortAliasFilter.matcher(str).matches();
				}).collect(Collectors.toSet()));
		private final Supplier<Pattern> shortAliasPattern = Suppliers.memoize(() -> {
			final List<String> emoticons = Lists.newArrayList(this.shortAlias.get());
			//List of emotions should be pre-processed to handle instances of subtrings like :-) :-
			//Without this pre-processing, emoticons in a string won't be processed properly
			for (int i = 0; i<emoticons.size(); i++)
				for (int j = i+1; j<emoticons.size(); j++) {
					final String o1 = emoticons.get(i);
					final String o2 = emoticons.get(j);
					if (o2.contains(o1)) {
						final String temp = o2;
						emoticons.remove(j);
						emoticons.add(i, temp);
					}
				}
			final String emojiFilter = emoticons.stream().map(Pattern::quote).collect(Collectors.joining("|"));
			return Pattern.compile(String.format("(?<=^| )(?:%s)(?= |$)", emojiFilter));
		});
		private static final Pattern utfFilter = Pattern.compile(".+[\uD83C\uDFFB-\uD83C\uDFFF]$");
		private final Supplier<Set<String>> utf = Suppliers.memoize(() -> this.utfDictionary.keySet().stream()
				.filter(str -> {
					return !utfFilter.matcher(str).matches();
				}).collect(Collectors.toSet()));
		private final Supplier<Pattern> utfPattern = Suppliers.memoize(() -> {
			final String emojiFilter = this.utf.get().stream().map(Pattern::quote).collect(Collectors.joining("|"));
			final String toneFilter = "[\uD83C\uDFFB-\uD83C\uDFFF]";
			return Pattern.compile(String.format("(?:%s)%s?", emojiFilter, toneFilter));
		});

		public StandardEmojiIdDictionaryBuilder putAllAlias(final Map<String, EmojiId> dictionary) {
			this.aliasDictionary.putAll(dictionary);
			return this;
		}

		public StandardEmojiIdDictionaryBuilder putAllUtf(final Map<String, EmojiId> dictionary) {
			this.utfDictionary.putAll(dictionary);
			return this;
		}

		public StandardEmojiIdDictionaryBuilder putAlias(final String key, final EmojiId emojiId) {
			this.aliasDictionary.put(key, emojiId);
			return this;
		}

		public StandardEmojiIdDictionaryBuilder putUtf(final String key, final EmojiId emojiId) {
			this.utfDictionary.put(key, emojiId);
			return this;
		}

		public StandardEmojiIdDictionary build() {
			return new StandardEmojiIdDictionary(
					ImmutableMap.copyOf(this.aliasDictionary),
					ImmutableMap.copyOf(this.utfDictionary),
					this.shortAliasPattern.get(),
					this.utfPattern.get());
		}
	}

	public static class StandardEmojiIdRepository {
		public static StandardEmojiIdDictionary instance = new StandardEmojiIdDictionaryBuilder().build();
	}
}