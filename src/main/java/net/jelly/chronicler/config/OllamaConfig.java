package net.jelly.chronicler.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class OllamaConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> OLLAMA_PATH;
    public static final ForgeConfigSpec.ConfigValue<String> OLLAMA_MODEL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Ollama GM Configuration").push("ollama");

        OLLAMA_PATH = builder
                .comment("Path to the ollama executable. Use 'ollama' if it's on your system PATH, or provide a full path like 'C:/Users/you/AppData/Local/Programs/Ollama/ollama.exe'")
                .define("executablePath", "ollama");

        OLLAMA_MODEL = builder
                .comment("The Ollama model to use for generation.")
                .define("model", "llama3");

        builder.pop();
        SPEC = builder.build();
    }
}
