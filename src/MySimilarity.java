import org.apache.lucene.search.similarities.*;
import java.math.*;
public class MySimilarity extends DefaultSimilarity{
	
	public float tf(float freq)
	{
		return (float)(1+Math.log(freq));
		/*if(freq>0){
			return 1;
		}else{
		 return 0;
		}*/

	}
	public float idf(long docFreq,long numDocs)
	{
		return (Math.max(0,(float)Math.log((numDocs)/(docFreq + 1))));
	//	return 1;
	}
	}
