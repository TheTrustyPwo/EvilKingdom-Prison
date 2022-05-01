package net.minecraft.util;

import java.util.function.Supplier;
import org.apache.commons.lang3.ObjectUtils;

public record ModCheck(ModCheck.Confidence confidence, String description) {
    public static ModCheck identify(String vanillaBrand, Supplier<String> brandSupplier, String environment, Class<?> clazz) {
        String string = brandSupplier.get();
        if (!vanillaBrand.equals(string)) {
            return new ModCheck(ModCheck.Confidence.DEFINITELY, environment + " brand changed to '" + string + "'");
        } else {
            return clazz.getSigners() == null ? new ModCheck(ModCheck.Confidence.VERY_LIKELY, environment + " jar signature invalidated") : new ModCheck(ModCheck.Confidence.PROBABLY_NOT, environment + " jar signature and brand is untouched");
        }
    }

    public boolean shouldReportAsModified() {
        return this.confidence.shouldReportAsModified;
    }

    public ModCheck merge(ModCheck brand) {
        return new ModCheck(ObjectUtils.max(this.confidence, brand.confidence), this.description + "; " + brand.description);
    }

    public String fullDescription() {
        return this.confidence.description + " " + this.description;
    }

    public static enum Confidence {
        PROBABLY_NOT("Probably not.", false),
        VERY_LIKELY("Very likely;", true),
        DEFINITELY("Definitely;", true);

        final String description;
        final boolean shouldReportAsModified;

        private Confidence(String description, boolean modded) {
            this.description = description;
            this.shouldReportAsModified = modded;
        }
    }
}
