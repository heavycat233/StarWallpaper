#version 300 es
precision mediump float;
in vec2 vUv;
uniform sampler2D uTexture;
uniform int uFlip;
out vec4 fragColor;
void main(){fragColor=texture(uTexture,vec2(vUv.x,uFlip==1?1.-vUv.y:vUv.y));}
