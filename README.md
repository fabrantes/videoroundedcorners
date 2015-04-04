Sample Android code that displays video with rounded corners.

Classes:

 - MainActivity - example activity that show the code in action
 - WickedGradientDrawable - just a tiny drawable that draws an ever changing gradient so we can see
  that the videos are actually translucent. Relevant only for demonstration purposes.
 - VideoSurfaceView - GLSurfaceView subclass that bridges a MediaPlayer and rounded video on the
  screen
 - GLRoundedGeometry - Utility class that creates the GL geometry where the video frames will be
  mapped on the GL viewport.