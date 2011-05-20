import java.util.*;
import java.io.*;

public class smd2obj
{
	public static class Triangle
	{
		public Material material;
		public Vertex[] vertIDs;
		public TextureCoords[] texIDs;
		public Normal[] normIDs;
		
		public Triangle()
		{
			vertIDs = new Vertex[3];
			texIDs = new TextureCoords[3];
			normIDs = new Normal[3];
		}

		public void append(PrintWriter f)
		{
			f.printf("f %d/%d/%d %d/%d/%d %d/%d/%d",
					vertIDs[0].id, texIDs[0].id, normIDs[0].id,
					vertIDs[1].id, texIDs[1].id, normIDs[1].id,
					vertIDs[2].id, texIDs[2].id, normIDs[2].id
					);
		}
	}
	
	public static class Vertex
	{
		public double x, y, z;
		public int id;

		public boolean equals(Object obj)
		{
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Vertex other = (Vertex) obj;
			if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
				return false;
			}
			if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
				return false;
			}
			if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z)) {
				return false;
			}
			return true;
		}
		
		public int hashCode()
		{
			return id;
		}
		
		public void append(PrintWriter b) throws IOException
		{
			b.print("v ");
			b.print(x);
			b.print(" ");
			b.print(y);
			b.print(" ");
			b.print(z);
		}
	}
	
	public static class TextureCoords extends Vertex
	{
		public void append(PrintWriter b) throws IOException
		{
			b.print("vt ");
			b.print(x);
			b.print(" ");
			b.print(y);
		}
	}
	
	public static class Normal extends Vertex
	{
		public void append(PrintWriter b) throws IOException
		{
			b.print("vn ");
			b.print(x);
			b.print(" ");
			b.print(y);
			b.print(" ");
			b.print(z);
		}
	}

	public static class Material
	{
		public String originalname;
		public String filename;
		public String matname;

		public Material(String filename)
		{
			this.originalname = filename;
			String newname = filename.substring(filename.lastIndexOf('/')+1, filename.lastIndexOf('.'))+".png";
			this.filename = "textures/"+newname;
			matname = newname;
		}

		public void append(PrintWriter b)
		{
			b.println("newmtl "+matname);
			b.println("Ka 1 1 1");
			b.println("Kd 1 1 1");
			b.println("Ks 0 0 0");
			b.println("illum 1");
			b.println("map_Ka "+filename);
			b.println("map_Kd "+filename);
			b.println("map_Ks "+filename);
		}

		public int hashCode() { return filename.hashCode(); }

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Material other = (Material) obj;
			if ((this.matname == null) ? (other.matname != null) : !this.matname.equals(other.matname)) {
				return false;
			}
			return true;
		}
	}
	
	public static List<Vertex> verts;
	public static List<TextureCoords> textures;
	public static List<Normal> norms;
	public static Map<String,Material> materials;
	public static Collection<Triangle> triangles;

	public static void main(String[] args)
	{
		verts = new ArrayList<Vertex>();
		textures = new ArrayList<TextureCoords>();
		norms = new ArrayList<Normal>();
		materials = new HashMap<String,Material>();
		triangles = new LinkedList<Triangle>();
		
		if(args.length != 2)
		{
			System.err.println("Usage: java smd2obj <infile> <outfile>");
			return;
		}
		
		Scanner in;
		PrintWriter outfile;
		PrintWriter materialfile;
		String matlibname = args[1] + ".mtl";
		try
		{
			in = new Scanner(new File(args[0]));
			outfile = new PrintWriter(new FileOutputStream(args[1]));
			materialfile = new PrintWriter(new FileOutputStream(matlibname));
		}
		catch(IOException e)
		{
			System.err.println("Error while opening file: "+e.getMessage());
			return;
		}

		while(in.hasNextLine() && !in.nextLine().equals("triangles")) {}
		if(!in.hasNextLine())
		{
			System.err.println("Didn't find a mesh beginning.");
			return;
		}

		System.out.println("Found triangles section, begin parsing.");
		try
		{
			while(parseTriangle(in)) {}
		}
		catch(IOException e)
		{
			System.err.println("IO Error: "+e.getMessage());
			return;
		}
		// Catch scanner stuff
		catch(InputMismatchException e)
		{
			System.err.println("Malformed SMD file.");
			return;
		}
		catch(NoSuchElementException e)
		{
			System.err.println("Malformed SMD file.");
			return;
		}
		
		in.close();
		in = null;

		try
		{
			System.out.println("Finished parsing, writing output file.");

			outfile.println("# Decompiled with SMD2OBJ by Colonel Thirty Two\n");
			outfile.println("mtllib "+matlibname);
			outfile.println("# BEGIN VERTICIES");
			int index = 1;
			for(Vertex v : verts)
			{
				v.append(outfile);
				outfile.println();
				v.id = index++;
			}
			System.out.println("Wrote "+(index-1)+" verticies");
			outfile.println("# END VERTICIES\n");

			outfile.flush();

			outfile.println("# BEGIN TEXTURE COORDINATES");
			index = 1;
			for(TextureCoords c : textures)
			{
				c.append(outfile);
				outfile.println();
				c.id = index++;
			}
			System.out.println("Wrote "+(index-1)+" texure coordinates");
			outfile.println("# END TEXTURE COORDINATES\n");

			outfile.flush();

			outfile.println("# BEGIN VERTEX NORMALS");
			index = 1;
			for(Normal c : norms)
			{
				c.append(outfile);
				outfile.println();
				c.id = index++;
			}
			System.out.println("Wrote "+(index-1)+" vertex normals");
			outfile.println("# END VERTEX NORMALS\n");

			outfile.flush();

			Material currentMaterial = null;
			outfile.println("# BEGIN FACES");
			index = 1;
			for(Triangle t : triangles)
			{
				if(!t.material.equals(currentMaterial))
				{
					currentMaterial = t.material;
					outfile.println("usemtl "+t.material.matname);
				}
				t.append(outfile);
				outfile.println();
				index++;
			}
			System.out.println("Wrote "+(index-1)+" mesh faces");
			outfile.println("# END FACES\n");

			outfile.flush();
			outfile.close();

			System.out.println("Finished writing mesh file, begin writing material file.");
			materialfile.println();
			index = 1;
			for(Material m : materials.values())
			{
				materialfile.println("# "+m.originalname);
				m.append(materialfile);
				materialfile.println();
				index++;
			}
			System.out.println("Wrote "+(index-1)+" materials");
			materialfile.flush();
			materialfile.close();

			System.out.println("Finished converting model.");

		}
		catch(IOException e)
		{
			System.err.println("IO Error: "+e.getMessage());
			return;
		}
	}

	private static boolean parseTriangle(Scanner in) throws IOException
	{
		if(!in.hasNext())
		{
			System.err.println("SMD file does not end triangles section.");
			throw new IOException();
		}

		String tex = in.nextLine();
		if(tex.equals("end")) return false;

		Triangle t = new Triangle();
		
		if(materials.get(tex) == null) materials.put(tex, new Material(tex));
		t.material = materials.get(tex);

		for(int i=0; i<3; i++) { parseVertex(in, t, i); }

		triangles.add(t);

		return true;
	}

	private static void parseVertex(Scanner in, Triangle t, int i)
	{
		Vertex v = new Vertex();
		TextureCoords c = new TextureCoords();
		Normal n = new Normal();

		int parentbone = in.nextInt();

		v.x = in.nextDouble();
		v.y = in.nextDouble();
		v.z = in.nextDouble();

		int vindex = verts.indexOf(v);
		if(vindex != -1) v = verts.get(vindex);
		else verts.add(v);

		n.x = in.nextDouble();
		n.y = in.nextDouble();
		n.z = in.nextDouble();

		int nindex = norms.indexOf(n);
		if(nindex != -1) n = norms.get(nindex);
		else norms.add(n);

		c.x = in.nextDouble();
		c.y = in.nextDouble();
		c.z = 0;

		int zindex = textures.indexOf(c);
		if(zindex != -1) c = textures.get(zindex);
		else textures.add(c);

		t.vertIDs[i] = v;
		t.texIDs[i] = c;
		t.normIDs[i] = n;

		in.nextLine();
	}

	private smd2obj() {
	}
}