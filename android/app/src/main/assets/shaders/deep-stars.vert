#version 300 es
precision highp float;
layout(location=0) in vec3 aVector;
layout(location=1) in float aMagnitude;
layout(location=2) in float aColorIndex;
uniform mat3 uLocal;
uniform vec3 uRight,uUp,uCenter;
uniform vec2 uSize;
uniform float uScale,uPixelScale,uFaintBrightness;
out vec3 vColor;
out float vAlpha;
void main(){
    vec3 v=uLocal*aVector;float z=dot(v,uCenter);
    if(v.z<=0.||z<-.001){gl_Position=vec4(3.,3.,0.,1.);gl_PointSize=1.;vAlpha=0.;vColor=vec3(0.);return;}
    vec2 p=vec2(dot(v,uRight),dot(v,uUp));float r=length(p),theta=acos(clamp(z,-1.,1.));
    p*=r<.000001?uScale:uScale*theta/r;
    gl_Position=vec4(p/uSize*2.,0.,1.);
    gl_PointSize=2.;
    float magnitude=aMagnitude;
    float size=max(.28,1.7-magnitude*.17)*uPixelScale;
    float gain=mix(1.,uFaintBrightness,smoothstep(5.,8.,magnitude));
    float power=clamp(1.6*pow(10.,-.115*max(-1.,magnitude)),.08,1.)*gain*min(1.,3.14159265*size*size);
    vAlpha=min(1.,power*.25);
    if(aColorIndex<0.)vColor=vec3(174.,198.,255.)/255.;
    else if(aColorIndex<.4)vColor=vec3(204.,218.,255.)/255.;
    else if(aColorIndex<.85)vColor=vec3(239.,236.,225.)/255.;
    else if(aColorIndex<1.4)vColor=vec3(250.,221.,184.)/255.;
    else vColor=vec3(255.,190.,151.)/255.;
}
