#version 300 es
precision highp float;
precision highp int;
in vec2 vUv;
out vec4 fragColor;
uniform mat3 uInverse;
uniform vec3 uRight,uUp,uCenter;
uniform vec2 uSize;
uniform float uScale,uGlow,uDust;
float hash(vec3 p){uvec3 q=uvec3(ivec3(p));uint h=q.x*374761393u+q.y*668265263u+q.z*2147483647u;h=(h^(h>>13))*1274126177u;return float(h^(h>>16))/4294967295.;}
float noise(vec3 p){vec3 i=floor(p),f=fract(p);f=f*f*(3.-2.*f);return mix(mix(mix(hash(i),hash(i+vec3(1,0,0)),f.x),mix(hash(i+vec3(0,1,0)),hash(i+vec3(1,1,0)),f.x),f.y),mix(mix(hash(i+vec3(0,0,1)),hash(i+vec3(1,0,1)),f.x),mix(hash(i+vec3(0,1,1)),hash(i+vec3(1,1,1)),f.x),f.y),f.z);}
void main(){
    vec2 p=(vUv-.5)*uSize/uScale;float theta=length(p),s=theta<.00001?1.:sin(theta)/theta;
    vec3 local=uCenter*cos(theta)+(uRight*p.x+uUp*p.y)*s;
    if(local.z<=0.){fragColor=vec4(.012,.023,.047,1.);return;}
    vec3 q=uInverse*local;
    vec3 gal=mat3(-.0548755604,.4941094279,-.8676661490,-.8734370902,-.4448296300,-.1980763734,-.4838350155,.7469822445,.4559837762)*q;
    float l=atan(gal.y,gal.x),b=asin(clamp(gal.z,-1.,1.));
    float n1=noise(q*11.+vec3(23,14,7)),n2=noise(q*37.+vec3(71,53,17)),n3=noise(q*95.+vec3(59,2,43));
    float turbulence=n1*.55+n2*.3+n3*.15,lat=b+(n1-.5)*.08;
    float broad=exp(-lat*lat/.035),middle=exp(-lat*lat/.009),narrow=exp(-lat*lat/.0017);
    float core=exp(-l*l/.8),density=.72+core*1.3+.3*sin(l*3.+1.);
    float d=b-.018*sin(l*5.)-(n2-.5)*.045;
    float lane=exp(-d*d/.00085),dark=lane*(.46+.5*n1)*uDust;
    float g=max(0.,(broad*10.+middle*20.*turbulence+narrow*17.*turbulence)*density*(1.-min(.94,dark))*uGlow);
    float vignette=max(.43,1.-dot(vUv-.5,vUv-.5)*1.05),horizon=clamp(local.z*8.,.15,1.);
    vec3 rgb=(vec3(5,10,23)+g*vec3(.84+core*.28,.88+core*.12,1.17))*vignette*horizon/255.;
    fragColor=vec4(rgb,1.);
}
