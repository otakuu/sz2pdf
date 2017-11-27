package sz2pdf;

public class Memeory {

	public static void main(String[] args) {

	int imageId = 0;	
	
	for (int i = 0; i < 100; i++) {
		
		int iIsEven = i % 2;
		
		if(iIsEven==0){
			imageId++;
		}
		
		System.out.println("<figure id=\"legespiel_card_"+i+"\">");
		System.out.println("    <a href=\"#card_"+i+"\">");
		System.out.println("      <img class=\"boxFront\" src=\"./lib/"+imageId+".jpg\" />");
		System.out.println("      <img class=\"boxWhite\" src=\"./lib/shadow_card.png\" />");
		System.out.println("      <img class=\"boxBack\" src=\"./lib/back.jpg\"  />");
		System.out.println("       </a>");
		System.out.println("      <img class=\"boxStretch\" src=\"./lib/shim.gif\" />");
		System.out.println("</figure>");
		
	}	
	
		

	}

}
