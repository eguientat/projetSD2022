
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.*;
import java.util.Iterator;
import java.lang.Math.*;
import java.io.*;
import java.lang.Object;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.io.ArrayWritable;

  public class MRHITS {

    public MRHITS(){}

    public static class Node{
      public double authority;
      public double hub;
      public double id;
      public ArrayList<Node> enfants;
      public ArrayList<Node> parents;

      public Node(double id){
        this.authority=1;
        this.hub=1;
        this.id=id;
        this.enfants=new ArrayList<Node>();  //Node sur qui pointe la Node actuelle
        this.parents=new ArrayList<Node>(); //Node qui pointent vers la Node actuelle

      }

      public Node(double a,double h,double id){
        this.authority=a;
        this.hub=h;
        this.id=id;
        this.enfants=new ArrayList<Node>();  //Node sur qui pointe la Node actuelle
        this.parents=new ArrayList<Node>(); //Node qui pointent vers la Node actuelle

      }

      public double getHub(){
        return hub;
      }

      public double getAuth(){
        return authority;
      }

      public void setHub(double i){
        this.hub=i;
      }

      public void setAuth(double i){
        this.authority=i;
      }

    }


 

   

 




    public static class HITSMAP
       extends Mapper<Object, Text, Text, DoubleWritable>{

    private final static DoubleWritable one = new DoubleWritable();
    private Text word = new Text();
    
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException  {
      
      String s=value.toString();
      String[] parts=s.split("	");
      double nodeg,noded;
      nodeg=Double.parseDouble(parts[0]);
      noded=Double.parseDouble(parts[1]);

      Node droite=new Node(noded);

	if(noded==0){
		noded=0.01;
	}
	if(nodeg==0){
		nodeg=0.01;
	}

      for(int i=0;i<=1;i++){
         word.set(parts[i]);
          if(i==0){
            //ici positif car on envoie un enfant
            one.set(noded);
          }
          else{
            //negatif car on envoie un parent
            one.set(-nodeg);
          }  

        context.write(word,one);
      } 
    }
  }

  public static class HITSREDUCE
       extends Reducer<Text,DoubleWritable,Text,Text> {
    private Text result = new Text();
    private Text sommet= new Text();

  
    private ArrayList<String> listhub;
    private ArrayList<String> listeauth;

    private ArrayList<Node> listeNode;
    private ArrayList<Double> indiceNode;

    private double pos;

    protected void setup(Context context) throws IOException, InterruptedException {
        Configuration conf=context.getConfiguration();
      
      listhub=new ArrayList<String>();

      listeauth = new ArrayList<String>();

      listeNode = new ArrayList<Node>();
      indiceNode = new ArrayList<Double>();

      pos=0;

    }


    public void reduce(Text key, Iterable<DoubleWritable> values,
                       Context context
                       ) throws IOException, InterruptedException {
      
      
      Node n;
      String c=key.toString();
      double cle=Double.parseDouble(c);
      if(!indiceNode.contains(cle)){
        n=new Node(cle);
        indiceNode.add(cle);
        listeNode.add(n);
      } 
      
      int ind=indiceNode.indexOf(cle);
      n=listeNode.get(ind);
      
      
      for (DoubleWritable val : values) {
        pos = val.get();
        Node m;
        double ppos=pos;
        if((pos==0.01) || (pos==-0.01)){
        	ppos=0;
        }
        
        if(indiceNode.contains(Math.abs(ppos))){
          int inde=indiceNode.indexOf(cle);
          m=listeNode.get(inde);
        }  
        else{
          m=new Node(Math.abs(ppos));
          indiceNode.add(Math.abs(ppos));
          listeNode.add(m);
        } 
        
        if(pos<0){
          n.parents.add(m);
          m.enfants.add(n);
        }
        else{
          n.enfants.add(m);
          m.parents.add(n);
        }  

        
      }

      listeNode.set(ind,n);
      
    }



    public void cleanup(Context context) throws IOException, InterruptedException
        {
            ArrayList<Node> temp=listeNode;
            ArrayList<Node> templist=listeNode;


            //update authority et hub for temp
            int q=0;
            while(q!=1){ 
              for(Iterator<Node> i=temp.iterator();i.hasNext();){
                  double sumauth=0;
                  double sumhub=0;
                
                  Node x=i.next();
                  int posx=indiceNode.indexOf(x.id);
                    
                    
                  for(Iterator<Node> j=x.parents.iterator();j.hasNext();){
                    Node w=j.next();
                    int indexp=indiceNode.indexOf(w.id);
                    Node parent=listeNode.get(indexp);
                    sumauth+=parent.getHub();
                  }
                  
                  
                  for(Iterator<Node> j=x.enfants.iterator();j.hasNext();){
                    Node w=j.next();
                    int indexe=indiceNode.indexOf(w.id);
                    Node enfant=listeNode.get(indexe);
                    sumhub+=enfant.getAuth();

                  }
                  x.setAuth(sumauth);
                  x.setHub(sumhub);
                  templist.set(posx,x);

              }
              listeNode=templist; 
            q++;
            } 
            //fin de l'update



            //normalize
            double sh=1;
            double sa=1;
            //faire la somme des carrées de chaque hub et authority
            for(Iterator<Node> i=listeNode.iterator();i.hasNext();){
                Node n=i.next();
                double nhub=Math.pow(n.getHub(),2);
                sh=sh+nhub;
                double nauth=Math.pow(n.getAuth(),2);
                sa=sa+nauth;
            }
            sh=Math.sqrt(sh);
            sa=Math.sqrt(sa);
            //diviser chaque hub et authority de chaque node par la racine carre de la somme du carre de chaque hub et authority
            for(int i=0;i<listeNode.size();i++){
                Node n=listeNode.get(i);
                
                double h=n.getHub();
                h/=sh;
                n.setHub(h);

                double a=n.getAuth();
                a/=sa;
                n.setAuth(a);

                listeNode.set(i,n);
                result.set("Authority ="+n.getAuth()+" and Hub="+n.getHub());
                int identifiant=(int)n.id;
                sommet.set(""+identifiant);
                context.write(sommet, result);
            
            }




      }

  } 
  
  
  
  





  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "hits");
    job.setJarByClass(MRHITS.class);
    job.setMapperClass(HITSMAP.class);
    job.setReducerClass(HITSREDUCE.class);
    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(DoubleWritable.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    Path p=new Path(args[1]);
    FileOutputFormat.setOutputPath(job, p);
    System.exit(job.waitForCompletion(true) ? 0 : 1);
    
    
  }




}