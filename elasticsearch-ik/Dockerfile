FROM elasticsearch:7.17.3

# 从国内镜像下载 IK 分词器插件
RUN elasticsearch-plugin install --batch \
    https://github.moeyy.xyz/https://github.com/infinilabs/analysis-ik/releases/download/v7.17.3/elasticsearch-analysis-ik-7.17.3.zip
