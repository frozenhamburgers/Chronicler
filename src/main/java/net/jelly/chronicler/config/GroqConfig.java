package net.jelly.chronicler.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class GroqConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> GROQ_API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> GROQ_MODEL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Groq GM Configuration").push("groq");

        GROQ_API_KEY = builder
                .comment("Your Groq API key. Get one free at https://console.groq.com")
                .define("apiKey", "");

        GROQ_MODEL = builder
                .comment("Groq model to use. Options: llama-3.1-8b-instant, llama-3.1-70b-versatile, mixtral-8x7b-32768, gemma2-9b-it")
                .define("model", "llama-3.1-8b-instant");

        builder.pop();
        SPEC = builder.build();
    }
}