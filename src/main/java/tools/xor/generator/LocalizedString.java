/**
 * XOR, empowering Model Driven Architecture in J2EE applications
 *
 * Copyright (c) 2012, Dilip Dalton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package tools.xor.generator;

import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import tools.xor.Property;
import tools.xor.StringType;
import tools.xor.util.graph.StateGraph;

public class LocalizedString extends FixedSet
{

    private static Map<String, String> localeSpecificCharacters = new HashMap<>();

    static final Map<String, char[]> UnicodeRange = new HashMap<>();

    // initialize the locale specific characters
    static {

        // basic latin
        UnicodeRange.put("en", new char[]{0x0020, 0x007E});

        // Latin-1 Supplement
        UnicodeRange.put("fr", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("de", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("es", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("nl", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("fi", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("it", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("sv", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("pt", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("eu", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("da", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});
        UnicodeRange.put("no", new char[]{0x0020, 0x007E, 0x00A0, 0x00FF});

        // Latin Extended-A
        // Turkish
        UnicodeRange.put("tr", new char[]{0x0020, 0x007E, 0x0100, 0x0017F});
        UnicodeRange.put("pl", new char[]{0x0020, 0x007E, 0x0100, 0x0017F});

        // Latin Extended-B
        // Croatian, Romanian
        UnicodeRange.put("hr", new char[]{0x0020, 0x007E, 0x0180, 0x024F});
        UnicodeRange.put("cs", new char[]{0x0020, 0x007E, 0x0180, 0x024F});
        UnicodeRange.put("hu", new char[]{0x0020, 0x007E, 0x0180, 0x024F});
        UnicodeRange.put("ro", new char[]{0x0020, 0x007E, 0x0180, 0x024F});

        // Cyrillic
        // Bulgarian
        UnicodeRange.put("bg", new char[]{0x0401, 0x04FF});
        UnicodeRange.put("ru", new char[]{0x0401, 0x04FF});

        // Modern Greek
        // Greek
        UnicodeRange.put("el", new char[]{0x0370, 0x03FF});

        // Japanese (Hiragana, Katakana, Kanji)
        UnicodeRange.put("ja", new char[]{
                0x3000, 0x303F,
                0x3041, 0x3096, 0x3099, 0x309F, // Hiragana
                0x30A0, 0x30F7, 0x30FA, 0x30FF, // Katakana
                0xff00, 0xffef
            });

        // Korean
        UnicodeRange.put("ko", new char[]{ 0x1100, 0x11FF });

        // Chinese
        UnicodeRange.put("zh", new char[]{ 0x4e00, 0x9faf });

        // Pairs of code blocks
        UnicodeRange.put("ar", new char[]{0x0600, 0x06ff});


        // Below code is from Stack Overflow
        Locale[] allLocales = Locale.getAvailableLocales();
        for (Locale l : allLocales) {
            StringBuilder buf = new StringBuilder();

            String localeId = MessageFormat.format("{0}-{1}", l.getLanguage(), l.getCountry());
            if(l.getCountry() == null || "".equals(l.getCountry())) {
                localeId = MessageFormat.format("{0}", l.getLanguage());
            }

            // Since we currently ignore country, we will find duplicates
            if(localeSpecificCharacters.containsKey(localeId)) {
                continue;
            }

            if (UnicodeRange.containsKey(l.getLanguage())) {
                // calculate number of pairs
                // If there is only one code block, then there is 1 pair
                int numPairs = UnicodeRange.get(l.getLanguage()).length/2;
                for(int block = numPairs-1; block < numPairs; block++) {
                    for (char i = UnicodeRange.get(l.getLanguage())[block*2];
                         i <= UnicodeRange.get(l.getLanguage())[block*2+1]; i++) {
                        buf.append(i);
                    }
                }
                localeSpecificCharacters.put(localeId, buf.toString());
                continue;
            }
        }
    }

    static SecureRandom rnd = new SecureRandom();

    public LocalizedString (String[] arguments)
    {
        super(arguments);
    }

    public static String randomString( int len, final String language ){
        String localeChars = localeSpecificCharacters.get(language);

        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( localeChars.charAt( rnd.nextInt(localeChars.length()) ) );
        return sb.toString();
    }

    @Override
    public String getStringValue (Property property, StateGraph.ObjectGenerationVisitor visitor)
    {
        // Find the locale (e.g., en, ja etc)
        String localeStr = getValues()[visitor.getSequenceNo()];

        // Make it conformant with IETF BCP 47
        String localeNormalized = localeStr.replace("_", "-");
        Locale locale = Locale.forLanguageTag(localeNormalized);

        String language = locale.getLanguage();
        if(!localeSpecificCharacters.containsKey(language)) {
            language = Locale.getDefault().getLanguage();
        }

        return randomString(StringType.getLength((Integer) visitor.getContext()), language);
    }
}
