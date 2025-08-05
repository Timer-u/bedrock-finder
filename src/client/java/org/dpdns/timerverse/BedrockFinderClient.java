package org.dpdns.timerverse;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

public class BedrockFinderClient implements ClientModInitializer {

  private static final int[] Y_LEVELS = {4, 123}; // 要扫描的Y坐标
  private static final String OUTPUT_FILE_NAME = "bedrock_coordinates.txt";

  @Override
  public void onInitializeClient() {
    // 注册客户端命令
    ClientCommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess) -> {
          dispatcher.register(
              ClientCommandManager.literal("findbedrock")
                  .then(
                      ClientCommandManager.argument("count", IntegerArgumentType.integer(1))
                          .executes(
                              context -> {
                                final int count = IntegerArgumentType.getInteger(context, "count");
                                final MinecraftClient client = MinecraftClient.getInstance();

                                // 发送开始消息
                                if (client.player != null) {
                                  client.player.sendMessage(
                                      Text.literal("§a开始扫描基岩，目标数量: " + count + ". 这可能会需要一些时间..."),
                                      false);
                                }

                                // 在新线程中执行扫描，防止游戏卡顿
                                new Thread(() -> findBedrock(client, count)).start();

                                return 1; // 1 表示命令成功执行
                              })));
        });
  }

  private void findBedrock(MinecraftClient client, int targetCount) {
    if (client.player == null || client.world == null) {
      return; // 如果玩家或世界不存在则返回
    }

    Set<String> coordinates = new HashSet<>(); // 使用 HashSet 自动去重

    BlockPos playerPos = client.player.getBlockPos();
    int searchRadius = 0;

    try {
      // 以玩家为中心，一圈一圈向外扩展搜索
      while (coordinates.size() < targetCount) {
        boolean foundInRadius = false;
        int chunkX = playerPos.getX() >> 4;
        int chunkZ = playerPos.getZ() >> 4;

        for (int x = -searchRadius; x <= searchRadius; x++) {
          for (int z = -searchRadius; z <= searchRadius; z++) {
            // 只检查当前环形的区块，避免重复扫描
            if (Math.abs(x) < searchRadius && Math.abs(z) < searchRadius) {
              continue;
            }

            ChunkPos currentChunkPos = new ChunkPos(chunkX + x, chunkZ + z);
            WorldChunk chunk = client.world.getChunk(currentChunkPos.x, currentChunkPos.z);

            for (int chunkBlockX = 0; chunkBlockX < 16; chunkBlockX++) {
              for (int chunkBlockZ = 0; chunkBlockZ < 16; chunkBlockZ++) {
                for (int y : Y_LEVELS) {
                  BlockPos blockPos =
                      new BlockPos(
                          currentChunkPos.getStartX() + chunkBlockX,
                          y,
                          currentChunkPos.getStartZ() + chunkBlockZ);
                  if (chunk.getBlockState(blockPos).getBlock() == Blocks.BEDROCK) {
                    coordinates.add( // .add() 方法会自动处理重复
                        String.format(
                            "%d %d %d Bedrock", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
                    if (coordinates.size() >= targetCount) {
                      // 提前结束所有循环
                      finishSearch(client, coordinates);
                      return;
                    }
                    foundInRadius = true;
                  }
                }
              }
            }
          }
        }

        // 如果在一个搜索半径内一个基岩都没找到，并且半径已经很大，则可能区域内没有更多基岩，提前中止
        if (!foundInRadius && searchRadius > 10) {
          if (client.player != null) {
            final int finalSearchRadius = searchRadius;
            client.execute(
                () ->
                    client.player.sendMessage(
                        Text.literal("§e在半径 " + (finalSearchRadius * 16) + " 的范围内未找到更多基岩，已停止搜索。"),
                        false));
          }
          break;
        }
        searchRadius++;
      }

      // 完成搜索
      finishSearch(client, coordinates);

    } catch (Exception e) {
      e.printStackTrace();
      if (client.player != null) {
        client.execute(
            () -> client.player.sendMessage(Text.literal("§c扫描过程中发生错误: " + e.getMessage()), false));
      }
    }
  }

  private void finishSearch(MinecraftClient client, List<String> coordinates) {
    if (coordinates.isEmpty()) {
      if (client.player != null) {
        client.execute(() -> client.player.sendMessage(Text.literal("§c未找到任何基岩。"), false));
      }
      return;
    }

    try {
      // 写入文件
      Path outputPath = client.runDirectory.toPath().resolve(OUTPUT_FILE_NAME);
      Files.write(outputPath, coordinates);

      // 通过 client.execute() 将消息发送回主线程，确保线程安全
      if (client.player != null) {
        client.execute(
            () ->
                client.player.sendMessage(
                    Text.literal(
                        "§a成功找到 "
                            + coordinates.size()
                            + " 个基岩坐标，已保存至文件: "
                            + outputPath.toAbsolutePath()),
                    false));
      }

    } catch (IOException e) {
      e.printStackTrace();
      if (client.player != null) {
        client.execute(
            () -> client.player.sendMessage(Text.literal("§c无法写入文件: " + e.getMessage()), false));
      }
    }
  }
}
