const { setGlobalOptions } = require("firebase-functions");
setGlobalOptions({ maxInstances: 10 });

const { onCall } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const OpenAI = require("openai");

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const LABELS = `
Amphibolite
Andesite
Anthracite
Basalt
Blueschist
Breccia
Carbonatite
Chalk
Chert
Coal
Conglomerate
Diamictite
Eclogite
Evaporite
Flint
Gabbro
Gneiss
Granite
Granulite
Greenschist
Greywacke
Hornfels
Komatiite
Limestone
Marble
Migmatite
Mudstone
Obsidian
Oil_shale
Oolite
Pegmatite
Phyllite
Porphyry
Pumice
Pyroxenite
Quartz_diorite
Quartz_monzonite
Quartzite
Quartzolite
Rhyolite
Sandstone
Scoria
Serpentinite
Shale
Siltstone
Slate
Talc_carbonate
Tephrite
Tuff
Turbidite
Wackestone
`;

// Parse labels once
const ALLOWED_LABELS = LABELS
  .split("\n")
  .map((s) => s.trim())
  .filter(Boolean);

exports.scanRockImage = onCall(
  {
    region: "us-central1",
    secrets: [OPENAI_API_KEY],
  },
  async (request) => {
    const base64Image = request.data?.image;
    if (!base64Image) {
      throw new Error("No image provided (data.image is missing).");
    }

    const openai = new OpenAI({ apiKey: OPENAI_API_KEY.value() });

    // Force JSON output so we can reliably extract confidence
    const prompt = `
Identify the rock in this image.

Return ONLY valid JSON in exactly this format:
{"rockName":"Granite","confidence":0.87}

Rules:
- rockName MUST be exactly one from this list:
${ALLOWED_LABELS.join(", ")}
- confidence MUST be a number from 0 to 1
- No extra keys, no explanation, no markdown.
`;

    const response = await openai.responses.create({
      model: "gpt-4o-mini",
      input: [
        {
          role: "user",
          content: [
            { type: "input_text", text: prompt },
            {
              type: "input_image",
              image_url: `data:image/jpeg;base64,${base64Image}`,
            },
          ],
        },
      ],
    });

    const rawText = response.output_text?.trim() ?? "{}";

    // Defaults if parsing fails
    let rockName = "Unknown";
    let confidence = 0;

    try {
      const parsed = JSON.parse(rawText);

      const candidate = (parsed.rockName ?? "").toString().trim();
      const conf = Number(parsed.confidence);

      // Validate label against allowed list (case-insensitive)
      const match = ALLOWED_LABELS.find(
        (label) => label.toLowerCase() === candidate.toLowerCase()
      );

      rockName = match ?? "Unknown";

      // Clamp confidence to [0, 1]
      confidence = Number.isFinite(conf) ? Math.min(1, Math.max(0, conf)) : 0;
    } catch (e) {
      // If model returns non-JSON, keep defaults
      // (Optional) console.log("JSON parse failed:", rawText);
    }

    return { rockName, confidence };
  }
);
