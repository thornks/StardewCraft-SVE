const fs = require('fs');
const path = require('path');
const { PNG } = require('pngjs');

const root = path.resolve(__dirname, '..');
const baseAssets = path.resolve(root, '..', 'stardewcraft_jar', 'assets', 'stardewcraft', 'textures', 'item', 'artisan');
const textures = path.join(root, 'src', 'main', 'resources', 'assets', 'stardewcraftsve', 'textures', 'item', 'artisan');
const models = path.join(root, 'src', 'main', 'resources', 'assets', 'stardewcraftsve', 'models', 'item');

const products = [
  ['joja_berry', 'wine', '#2d7bd3'],
  ['monster_fruit', 'wine', '#8f46c8'],
  ['salal_berry', 'wine', '#6f37a5'],
  ['slime_berry', 'wine', '#8fae35'],
  ['cucumber', 'juice', '#65a83d'],
  ['butternut_squash', 'juice', '#d78c2e'],
  ['gold_carrot', 'juice', '#f0c83a'],
  ['sweet_potato', 'juice', '#bd5748'],
  ['joja_veggie', 'juice', '#58aa4a'],
  ['ancient_fiber', 'juice', '#708b50'],
  ['monster_mushroom', 'juice', '#d67b38'],
  ['void_root', 'juice', '#56357f']
];

function rgb(hex) {
  return [1, 3, 5].map(offset => parseInt(hex.slice(offset, offset + 2), 16));
}

function recolor(sourcePath, targetPath, color, type) {
  const image = PNG.sync.read(fs.readFileSync(sourcePath));
  const target = rgb(color);
  for (let offset = 0; offset < image.data.length; offset += 4) {
    const [red, green, blue, alpha] = image.data.subarray(offset, offset + 4);
    if (alpha === 0) continue;
    const isLiquid = type === 'wine'
      ? red > green * 2
      : blue <= 20 && Math.abs(red - green) <= 10;
    if (!isLiquid) continue;

    const intensity = Math.min(1, (type === 'wine' ? red / 219 : Math.max(red, green) / 233));
    image.data[offset] = Math.round(target[0] * intensity);
    image.data[offset + 1] = Math.round(target[1] * intensity);
    image.data[offset + 2] = Math.round(target[2] * intensity);
  }
  fs.mkdirSync(path.dirname(targetPath), { recursive: true });
  fs.writeFileSync(targetPath, PNG.sync.write(image));
}

function writeJson(targetPath, value) {
  fs.mkdirSync(path.dirname(targetPath), { recursive: true });
  fs.writeFileSync(targetPath, JSON.stringify(value, null, 2) + '\n');
}

for (const [crop, type, color] of products) {
  const sourceName = type === 'wine' ? 'strawberry.png' : 'parsnip.png';
  const outputName = `${crop}_${type}`;
  const texture = `stardewcraftsve:item/artisan/${type}/${crop}`;
  recolor(
    path.join(baseAssets, type, sourceName),
    path.join(textures, type, `${crop}.png`),
    color,
    type
  );

  if (type === 'juice') {
    writeJson(path.join(models, `${outputName}.json`), {
      parent: 'minecraft:item/generated',
      textures: { layer0: texture, particle: texture }
    });
    continue;
  }

  writeJson(path.join(models, `${outputName}.json`), {
    parent: 'minecraft:item/generated',
    textures: { layer0: texture, particle: texture },
    overrides: ['silver', 'gold', 'iridium'].map((quality, index) => ({
      predicate: { custom_model_data: index + 1 },
      model: `stardewcraftsve:item/${outputName}_${quality}`
    }))
  });
  for (const quality of ['silver', 'gold', 'iridium']) {
    writeJson(path.join(models, `${outputName}_${quality}.json`), {
      parent: 'minecraft:item/generated',
      textures: {
        layer0: texture,
        layer1: `stardewcraft:item/quality/${quality}_star`,
        particle: texture
      }
    });
  }
}

console.log(`Generated ${products.length} SVE keg product asset sets.`);
