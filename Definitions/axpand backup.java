			private void expand(Node node, HashMap<Node, Integer> nodeSize,HashSet<Node> expanded, HashSet<Node> fixedSize1Nodes) {
				//expand up
//				if(node.children.isEmpty()){
//					if(node.parents.isEmpty()){
//						nodeSize.put(node, 1);//size 1 if not subnodes nor mapped
//					}else{
//						
//					}
//				}else{
				if(node.parents.size()==1){
					if(node.parents.get(0).children.get(0)==node||node.parents.get(0).children.get(node.parents.get(0).children.size()-1)==node){
						fixedSize1Nodes.add(node);
					}
					
				}
				//expand ins
				int parentsSize=0;
				for(Node parent:node.parents){
					if (!expanded.contains(node)){
						if (!nodeSize.containsKey(parent)){
							nodeSize.put(parent, 1);//size 1 if not subnodes nor mapped
						}else{
							if(!fixedSize1Nodes.contains(parent)){
								nodeSize.put(parent, nodeSize.get(parent)+1);
							}
						}
					}
					expand(parent,nodeSize,expanded, fixedSize1Nodes);
					if(node.parents.size()!=1){
						parentsSize+=nodeSize.get(parent);
					}
				}
				if (!nodeSize.containsKey(node)){
					nodeSize.put(node, 1);//size 1 if not subnodes nor mapped
				}
				if(nodeSize.get(node)<parentsSize){
					if(!fixedSize1Nodes.contains(node)){
						nodeSize.put(node, parentsSize);
					}
				}
				expanded.add(node);
//				}
//				//expand outs
//				int childrenSize=0;
//				for(Node child:node.children){
//					if(!nodeSize.containsKey(child)){
//						expand(child,nodeSize);
//					}
//					if(child.parents.size()==1){
//						childrenSize+=nodeSize.get(child);	
//					}
//				}
//				if(nodeSize.get(node)<childrenSize){
//					nodeSize.put(node, childrenSize);
//				}
//				if(node.parents.size()>1&&nodeSize.get(node)>parentsSize){
//					nodeSize.put(node.parents.get(0),(childrenSize-parentsSize)/2+3);//minimum size for a node divided in subnodes is 3
//					nodeSize.put(node.parents.get(node.parents.size()-1),(childrenSize-parentsSize)/2+3);//minimum size for a node divided in subnodes is 3
//					expand(node.parents.get(0),nodeSize);
//					expand(node.parents.get(node.parents.size()-1),nodeSize);
//					//expand ins
//					parentsSize=0;
//					for(Node parent:node.parents){
//						if(!nodeSize.containsKey(parent)){
//							expand(parent,nodeSize);
//						}
//						if(node.parents.size()!=1){
//							parentsSize+=nodeSize.get(parent);
//						}
//					}
//					if(nodeSize.get(node)<parentsSize){
//						nodeSize.put(node, parentsSize);
//					}
//				}
//				if(node.childrenAreSubnodes()){
//					nodeSize.put(node.children.get(node.children.size()/2), nodeSize.get(node)-childrenSize+nodeSize.get(node.children.get(node.children.size()/2)));
//					expand(node.children.get(node.children.size()/2),nodeSize);
//				}
				if(node.outOfInstance!=null){//the node is out of instance
					if(node.outOfInstance.definition.name=="nand"){//NAND //TODO: fix nand checking
						//this is out
						//expand ins
//						if(!nodeSize.containsKey(node.outOfInstance.in.get(0))||nodeSize.get(node.outOfInstance.in.get(0))<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(0), nodeSize, expanded, fixedSize1Nodes);
//						}
//						if(!nodeSize.containsKey(node.outOfInstance.in.get(1))||nodeSize.get(node.outOfInstance.in.get(1))<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(1), nodeSize, expanded, fixedSize1Nodes);
//						}
						if(nodeSize.get(node.outOfInstance.in.get(0))>nodeSize.get(node.outOfInstance.in.get(1))){
							nodeSize.put(node, nodeSize.get(node.outOfInstance.in.get(0)));
							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(1), nodeSize, fixedSize1Nodes, fixedSize1Nodes);
						}else if(nodeSize.get(node.outOfInstance.in.get(0))<nodeSize.get(node.outOfInstance.in.get(1))){
							nodeSize.put(node, nodeSize.get(node.outOfInstance.in.get(1)));
							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
							this.expand(node.outOfInstance.in.get(0), nodeSize, fixedSize1Nodes, fixedSize1Nodes);
						}else{
							nodeSize.put(node, nodeSize.get(node.outOfInstance.in.get(0)));
						}
//						int size0=nodeSize.get(node.outOfInstance.in.get(0));
//						int size1=nodeSize.get(node.outOfInstance.in.get(1));
//						if(nodeSize.get(node)<size0){
//							nodeSize.put(node, size0);
//						}
//						if(nodeSize.get(node)<size1){
//							nodeSize.put(node, size1);
//						}
//						if(size0<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(0), nodeSize.get(node));
//							this.expand(node.outOfInstance.in.get(0), nodeSize);
//						}
//						if (size1<nodeSize.get(node)){
//							nodeSize.put(node.outOfInstance.in.get(1), nodeSize.get(node));
//							this.expand(node.outOfInstance.in.get(1), nodeSize);
//						}
					}else{//the node is out of an instance different to NAND
						if(this.instances.contains(node.outOfInstance)){//check definition has not been removed (= is recursive) FIXME:Needed?
							HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
							//expand ins
							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
								this.expand(node.outOfInstance.in.get(i),nodeSize, expanded, fixedSize1Nodes);
								tempNodeSize.put(node.outOfInstance.definition.in.get(i),nodeSize.get(node.outOfInstance.in.get(i)));
							}
							node.outOfInstance.definition.expand(node.outOfInstance.definition.out.get(node.outOfInstance.out.indexOf(node)),tempNodeSize, expanded, fixedSize1Nodes);
							nodeSize.put(node, tempNodeSize.get(node.outOfInstance.definition.out.get(node.outOfInstance.out.indexOf(node))));
//							
							//expand outs
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								if(!nodeSize.containsKey(node.outOfInstance.out.get(i))){
//									node.outOfInstance.definition.expand(node.outOfInstance.out.get(i),nodeSize);
//								}
//								tempNodeSize.put(node.outOfInstance.definition.out.get(i),nodeSize.get(node.outOfInstance.out.get(i)));
//							}
//							
//							//expand definition
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								node.outOfInstance.definition.expand(node.outOfInstance.definition.out.get(i), tempNodeSize);
//							}
//							//expand outs if needed
//							for (int i = 0; i < node.outOfInstance.out.size(); i++) {
//								if(nodeSize.get(node.outOfInstance.out.get(i))<tempNodeSize.get(node.outOfInstance.definition.out.get(i))){
//									nodeSize.put(node.outOfInstance.out.get(i), tempNodeSize.get(node.outOfInstance.definition.out.get(i)));
//									if(node!=node.outOfInstance.definition.out.get(i)){
//										this.expand(node.outOfInstance.definition.out.get(i), tempNodeSize);
//									}
//								}
//							}
//							//expand ins if needed
//							for (int i = 0; i < node.outOfInstance.in.size(); i++) {
//								if(nodeSize.get(node.outOfInstance.in.get(i))<tempNodeSize.get(node.outOfInstance.definition.in.get(i))){
//									nodeSize.put(node.outOfInstance.in.get(i), tempNodeSize.get(node.outOfInstance.definition.in.get(i)));
//									expand(node.outOfInstance.in.get(i),nodeSize);
//								}
//							}
						}
						
					}
//					if(node.inOfInstances!=null){//the node is in of instance
//						for(Instance instance:node.inOfInstances){
//							if(this.instances.contains(instance)){//check definition has not been removed (= is recursive)
//								if(instance.definition.name=="nand"){//NAND //TODO: fix nand checking
//									if(!nodeSize.containsKey(instance.out.get(0))||nodeSize.get(instance.out.get(0))<nodeSize.get(node)){
//										nodeSize.put(instance.out.get(0), nodeSize.get(node));
//										this.expand(instance.out.get(0), nodeSize);
//									}
//									if(!nodeSize.containsKey(instance.in.get(0))||nodeSize.get(instance.in.get(0))<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(0), nodeSize.get(node));
//										if(node!=instance.in.get(0)){
//											this.expand(instance.in.get(0), nodeSize);
//										}
//									}
//									if(!nodeSize.containsKey(instance.in.get(1))||nodeSize.get(instance.in.get(1))<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(1), nodeSize.get(node));
//										if(node!=instance.in.get(1)){
//											this.expand(instance.in.get(1), nodeSize);
//										}
//									}
//									int size0=nodeSize.get(instance.in.get(0));
//									int size1=nodeSize.get(instance.in.get(1));
//									int sizeOut=nodeSize.get(instance.out.get(0));
//									if(nodeSize.get(node)<size0){
//										nodeSize.put(node, size0);
//									}
//									if(nodeSize.get(node)<size1){
//										nodeSize.put(node, size1);
//									}
//									if(nodeSize.get(node)<sizeOut){
//										nodeSize.put(node, sizeOut);
//									}
//									if(size0<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(0), nodeSize.get(node));
//										if(node!=instance.in.get(0)){
//											this.expand(instance.in.get(0), nodeSize);
//										}
//									}
//									if (size1<nodeSize.get(node)){
//										nodeSize.put(instance.in.get(1), nodeSize.get(node));
//										if(node!=instance.in.get(1)){
//											this.expand(instance.in.get(1), nodeSize);
//										}
//									}
//									if(sizeOut<nodeSize.get(node)){
//										nodeSize.put(instance.out.get(0), nodeSize.get(node));
//										this.expand(instance.out.get(0), nodeSize);
//									}
//								}else{//the node is in of an instance different to NAND
//									HashMap<Node, Integer> tempNodeSize = new HashMap<Node, Integer>();
//									//expand outs
//									for (int i = 0; i < instance.out.size(); i++) {
//										if(!nodeSize.containsKey(instance.out.get(i))){
//											this.expand(instance.out.get(i),nodeSize);
//										}
//										tempNodeSize.put(instance.definition.out.get(i),nodeSize.get(instance.out.get(i)));
//									}
//									//expand ins
//									for (int i = 0; i < instance.in.size(); i++) {
//										if(!nodeSize.containsKey(instance.in.get(i))){
//											this.expand(instance.in.get(i),nodeSize);
//										}
//										tempNodeSize.put(instance.definition.in.get(i),nodeSize.get(instance.in.get(i)));
//									}
//									//expand definition
//									for (int i = 0; i < instance.out.size(); i++) {
//										instance.definition.expand(instance.definition.out.get(i), tempNodeSize);
//									}
//									//expand outs if needed
//									for (int i = 0; i < instance.out.size(); i++) {
//										if(nodeSize.get(instance.out.get(i))<tempNodeSize.get(instance.definition.out.get(i))){
//											nodeSize.put(instance.out.get(i), tempNodeSize.get(instance.definition.out.get(i)));
//											this.expand(instance.definition.out.get(i), tempNodeSize);
//										}
//									}
//									//expand ins if needed
//									for (int i = 0; i < instance.in.size(); i++) {
//										if(nodeSize.get(instance.in.get(i))<tempNodeSize.get(instance.definition.in.get(i))){
//											nodeSize.put(instance.in.get(i), tempNodeSize.get(instance.definition.in.get(i)));
//											if(node!=instance.definition.in.get(i)){
//												expand(instance.in.get(i),nodeSize);
//											}
//										}
//									}
//									
//								}
//							}
//						}
//					}
				}
			}