#version 300 es
precision highp float;
layout(location=0) in vec3 aVector;
layout(location=1) in float aMagnitude;
layout(location=2) in float aColorIndex;
uniform mat3 uLocal;
uniform vec3 uRight,uUp,uCenter;
uniform vec2 uSize;
uniform float uScale,uTime,uPixelScale,uFaintBrightness,uBrightSize;
uniform int uTwinkle;
out vec3 vColor;
out float vAlpha;
out float vRadius,vExtent,vHalo,vArm,vLine,vWave;
void main(){
    vec3 v=uLocal*aVector;float z=dot(v,uCenter);
    if(v.z<0.||z<-.001){gl_Position=vec4(3,3,0,1);gl_PointSize=1.;vAlpha=0.;vColor=vec3(0);return;}
    vec2 p=vec2(dot(v,uRight),dot(v,uUp));float r=length(p),theta=acos(clamp(z,-1.,1.));
    p*=r<.000001?uScale:uScale*theta/r;
    gl_Position=vec4(p/uSize*2.,0.,1.);
    float size=max(.28,1.7-aMagnitude*.17)*uPixelScale*mix(1.,uBrightSize,clamp((6.5-aMagnitude)/3.5,0.,1.));
    vRadius=size;vHalo=aMagnitude<3.?size*4.3*(.8+.2*uBrightSize):0.;vArm=aMagnitude<.7?5.*uBrightSize*uPixelScale:0.;vLine=.55*uPixelScale;vExtent=max(size,max(vHalo,vArm))+1.;gl_PointSize=2.*vExtent;
    if(aColorIndex<0.)vColor=vec3(174,198,255)/255.;else if(aColorIndex<.4)vColor=vec3(204,218,255)/255.;else if(aColorIndex<.85)vColor=vec3(239,236,225)/255.;else if(aColorIndex<1.4)vColor=vec3(250,221,184)/255.;else vColor=vec3(255,190,151)/255.;
    float phase=float(gl_VertexID)*2.39996323;
    float strength=.08+.14*(1.-v.z),wave=uTwinkle==1&&aMagnitude<6.5?1.-strength+strength*sin(uTime*1.3+phase)*sin(uTime*.7+phase*1.7):1.;
    vWave=wave;
    float faintGain=mix(1.,uFaintBrightness,smoothstep(5.,8.,aMagnitude));
    vAlpha=clamp(1.6*pow(10.,-.115*max(-1.,aMagnitude)),.08,1.)*wave*faintGain;
}
