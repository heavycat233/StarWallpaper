#version 300 es
precision highp float;
in vec3 vColor;
in float vAlpha,vRadius,vExtent,vHalo,vArm,vLine,vWave;
out vec4 fragColor;
void main(){
 if(vAlpha==0.)discard;
 vec2 p=(gl_PointCoord-.5)*(2.*vExtent);float a=0.;
 // Canvas-style solid cores, linear halos and short cross strokes.
 for(int y=0;y<4;y++)for(int x=0;x<4;x++){
  vec2 q=p+(vec2(float(x),float(y))+.5)/4.-.5;float r=length(q);
  float h=vHalo>0.?.28*vWave*max(0.,1.-r/vHalo):0.;
  float core=r<vRadius?min(1.,vAlpha):0.;float alpha=core+h*(1.-core);
  bool crossMark=vArm>0.&&((abs(q.x)<=vArm&&abs(q.y)<=vLine*.5)||(abs(q.y)<=vArm&&abs(q.x)<=vLine*.5));
  if(crossMark)alpha=.23*vWave+alpha*(1.-.23*vWave);
  a+=alpha/16.;
 }
 if(a<=0.)discard;fragColor=vec4(vColor,a);
}
