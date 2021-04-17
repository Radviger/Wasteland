package com.legacy.wasteland.coremod.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Consumer;

public class Transformer implements IClassTransformer, Opcodes {
    private static Map<String, Consumer<ClassNode>> transformers = new HashMap<>();

    static {
        transformers.put("net.minecraft.client.renderer.EntityRenderer", Transformer::transformRainParticles);
        transformers.put("net.minecraft.world.WorldServer", Transformer::transformFillWithRain);
        transformers.put("net.minecraft.world.storage.loot.LootTableManager$Loader", Transformer::transformLoadLootTable);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) {
            return null;
        }
        Consumer<ClassNode> transformer = transformers.get(transformedName);
        if (transformer != null) {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            transformer.accept(node);

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            return writer.toByteArray();
        } else {
            return basicClass;
        }
    }

    private static void transformRainParticles(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals("addRainParticles") || method.name.equals("func_78484_h")) {
                method.instructions.clear();
                method.instructions.add(new MethodInsnNode(INVOKESTATIC, "com/legacy/wasteland/client/render/RenderWeather", "addRainParticles", "()V", false));
                method.instructions.add(new InsnNode(RETURN));
                break;
            }
        }
    }

    private static void transformFillWithRain(ClassNode classNode) {
        root:
        for (MethodNode method : classNode.methods) {
            if (method.name.equals("updateBlocks") || method.name.equals("func_147456_g")) {
                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode next = iterator.next();
                    if (next instanceof MethodInsnNode) {
                        MethodInsnNode m = (MethodInsnNode) next;
                        if ((m.name.equals("fillWithRain") || m.name.equals("func_176224_k"))
                                && m.owner.equals("net/minecraft/block/Block")) {

                            m.setOpcode(INVOKESTATIC);
                            m.name = "fillWithRain";
                            m.owner = "com/legacy/wasteland/CommonProxy";
                            m.desc = "(Lnet/minecraft/block/Block;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V";
                            break root;
                        }
                    }
                }
            }
        }
    }

    private static void transformLoadLootTable(ClassNode classNode) {
        root:
        for (MethodNode method : classNode.methods) {
            if ((method.name.equals("loadLootTable") || method.name.equals("func_186517_b"))) {
                ListIterator<AbstractInsnNode> iterator = method.instructions.iterator();
                while (iterator.hasNext()) {
                    AbstractInsnNode next = iterator.next();
                    if (next.getOpcode() == ALOAD) {
                        iterator.add(new VarInsnNode(ALOAD, 1));
                        iterator.add(new MethodInsnNode(INVOKESTATIC, "com/legacy/wasteland/Wasteland", "findLootTable", method.desc, false));
                        iterator.add(new InsnNode(DUP));
                        Label l0 = new Label();
                        iterator.add(new JumpInsnNode(IFNULL, new LabelNode(l0)));
                        iterator.add(new InsnNode(ARETURN));
                        iterator.add(new LabelNode(l0));
                        iterator.add(new InsnNode(POP));
                        break root;
                    }
                }
                break;
            }
        }
    }
}
