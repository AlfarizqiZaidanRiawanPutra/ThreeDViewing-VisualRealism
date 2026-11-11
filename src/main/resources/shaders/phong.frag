#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;

float calcShadow(vec4 fragPosLight, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLight.xyz / fragPosLight.w;
    proj = proj * 0.5 + 0.5;
    if (proj.z > 1.0) return 1.0;
    float closest = texture(shadowMap, proj.xy).r;
    float bias = max(0.05 * (1.0 - dot(norm, lightDir)), 0.005);
    return (proj.z > closest + bias) ? 0.2 : 1.0;
}

void main() {
    vec3 ambient = 0.2 * lightColor;

    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    vec3 viewDir = normalize(viewPos - FragPos);
    float spec;
    if (useBlinnPhong) {
        vec3 halfway = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfway), 0.0), 32.0);
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    }
    vec3 specular = 0.8 * spec * lightColor;

    float shadow = calcShadow(FragPosLightSpace, norm, lightDir);

    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadow * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
